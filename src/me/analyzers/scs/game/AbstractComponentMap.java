package me.analyzers.scs.game;

import me.analyzers.scs.primitiveComponents.*;
import me.analyzers.scs.utilities.Activation;
import me.analyzers.scs.utilities.Placeable;
import me.analyzers.scs.utilities.Rotation;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import static me.analyzers.scs.game.MainPanel.widthX;
import static me.analyzers.scs.game.MainPanel.widthY;
import static me.analyzers.scs.utilities.MathUtils.*;

public class AbstractComponentMap implements Serializable {
    //Holds the current ACM's component inputs.
    private final ConcurrentHashMap<ComponentHolder, boolean[][]> dynamicComponentInputs;

    //Holds all connections for every compiled component, by its output index.
    private final ConcurrentHashMap<ComponentHolder, ComponentConnection[][]> componentLinks;

    //Holds all ComponentHolders which must be updated next tick
    private Set<ComponentHolder> primed;

    //Ordered lists of I/O. Required for mapping an ACM another as a complex component (hookup is done through these)
    //"Components" without I/O pins shouldn't even be able to connect to components (no pins!)
    private final ArrayList<PrimitiveInput> inputPins;
    private final ArrayList<PrimitiveOutput> outputPins;

    //For making an ACM directly with these fields (used in deep cloning)
    public AbstractComponentMap(ConcurrentHashMap<ComponentHolder, boolean[][]> dynamicComponentInputs,
                                ConcurrentHashMap<ComponentHolder, ComponentConnection[][]> componentLinks,
                                Set<ComponentHolder> primed, ArrayList<PrimitiveInput> inputPins, ArrayList<PrimitiveOutput> outputPins) {
        this.dynamicComponentInputs = dynamicComponentInputs;
        this.componentLinks = componentLinks;
        this.primed = primed;
        this.inputPins = inputPins;
        this.outputPins = outputPins;
    }

    public AbstractComponentMap(Placeable[][] presenceMap, List<ComponentHolder> componentQueue) {
        /*
        The contract is such:
        guarantee all ACM state fields are properly setup.
        Flatten any complex components into this ACM, merging their ACM, omitting their in/out pins (unnecessary penalty)
        Display proper wire colors for toplevel wireLines.
         */

        inputPins = new ArrayList<>();
        outputPins = new ArrayList<>();

        //First, form I/O pins. Order is from left to right, up to down.
        for (int y = 0; y < widthY; y++) {
            for (int x = 0; x < widthX; x++) {
                Placeable placeable = presenceMap[x][y];
                if (placeable instanceof PrimitiveInput) {
                    inputPins.add((PrimitiveInput) placeable);
                } else if (placeable instanceof PrimitiveOutput) {
                    outputPins.add((PrimitiveOutput) placeable);
                }
            }
        }

        //Now, for each componentHolder, we'll begin a flood-fill that creates link and sets up their inputs.
        dynamicComponentInputs = new ConcurrentHashMap<>();
        componentLinks = new ConcurrentHashMap<>();

        for (ComponentHolder toClone : componentQueue) {
            //If the component is complex, make it clone its ACM as to avoid duplicate references in separate ACMs.
            if (!(toClone instanceof PrimitiveComponent)) {
                try {
                    toClone.cloneACM();
                } catch (CloneNotSupportedException e) {
                    System.err.println("Fatal error while cloning ACM template for " + toClone.getName() + ".");
                }
            }
        }

        for (ComponentHolder root : componentQueue) {
            if (!dynamicComponentInputs.containsKey(root)) {
                floodFill(root, presenceMap);
            }
        }

        //Now, insert the ACM of complex components while keeping the proper connections (remove primitive I/O)
        componentQueue.stream()
                .filter(c -> !(c instanceof PrimitiveComponent))
                .forEach(complexComponent -> {
                    //Merge the ACMs
                    AbstractComponentMap acm = complexComponent.getAcm();
                    ConcurrentHashMap<ComponentHolder, ComponentConnection[][]> complexComponentLinks = acm.getComponentLinks();
                    ConcurrentHashMap<ComponentHolder, boolean[][]> complexComponentInputs = acm.getDynamicComponentInputs();

                    //Copy array references so modifications to the top level ACM does not modify nested ACMs
                    for (ComponentHolder dci : complexComponentInputs.keySet()) {
                        //Reset all inputs to be floating
                        boolean[][] freshInputs = new boolean[complexComponentInputs.get(dci).length][];
                        Arrays.fill(freshInputs, new boolean[0]);
                        dynamicComponentInputs.put(dci, freshInputs);
                    }

                    for (ComponentHolder cl : complexComponentLinks.keySet()) {
                        //Shallow copy as second dimension is read-only, or is re-set to a new array.
                        componentLinks.put(cl, complexComponentLinks.get(cl).clone());
                    }

                    //All connections to this component were redirected in followWireLine()
                    dynamicComponentInputs.remove(complexComponent);

                    /*
                    Now, the outputs. Unfortunately, ACMs don't read backwards, so to get all connections to the outputs
                    (to redirect them) I will have to iterate through this complex component's connections, then change it in
                    the parent component. While a bit inefficient, this only has to be done once per ACM, and won't recurse to nested
                    components as their ACMs were already sanitized.
                     */

                    for (ComponentHolder possiblyConnectedComponent : complexComponentLinks.keySet()) {
                        //Index of the bundle of connections per output
                        for (int connectionsIndex = 0; connectionsIndex < complexComponentLinks.get(possiblyConnectedComponent).length; connectionsIndex++) {
                            //Index of the connection in this bundle
                            ComponentConnection[] connectionsToTheOutsideWorld = new ComponentConnection[0];
                            for (int connectionIndex = 0; connectionIndex < complexComponentLinks.get(possiblyConnectedComponent)[connectionsIndex].length; connectionIndex++) {
                                ComponentConnection connection = complexComponentLinks.get(possiblyConnectedComponent)[connectionsIndex][connectionIndex];
                                ComponentHolder primitiveOutput = connection.getComponent();

                                //Test if this connection is to an output
                                if (!(primitiveOutput instanceof PrimitiveOutput)) {
                                    //A normal connection; skip
                                    continue;
                                }

                                //OutputPins were made ordered, so get index of the output we are pointing to.
                                int outputIndex = acm.getOutputPins().indexOf(primitiveOutput);

                                try {
                                    //I love naming variables. Again, check for length one in case of a null-connected wire (side of component, etc.)
                                    connectionsToTheOutsideWorld = ArrayUtils.addAll(connectionsToTheOutsideWorld, componentLinks.get(complexComponent)[outputIndex]);
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    System.err.println("Error : Complex component "
                                            + complexComponent.getName() + " output connections length is not 1 for output " + outputIndex + ".");
                                }
                            }

                            //All connections to the outside world must share a wire line for proper displaying

                            HashSet<Wire> unifiedWireLine = new HashSet<>();
                            for (ComponentConnection connection : connectionsToTheOutsideWorld) {
                                if (!connection.isTopLevel()) { //Don't change "persistent" wireLines in the un-cloned templates
                                    continue;
                                }
                                unifiedWireLine.addAll(connection.getWireLine());
                            }

                            for (ComponentConnection connection : connectionsToTheOutsideWorld) {
                                if (!connection.isTopLevel()) {
                                    continue;
                                }
                                connection.getWireLine().clear();
                                connection.getWireLine().addAll(unifiedWireLine);
                            }

                            //Redirect in componentLinks
                            componentLinks.get(possiblyConnectedComponent)[connectionsIndex] =
                                    Stream.concat(Arrays.stream(componentLinks.get(possiblyConnectedComponent)[connectionsIndex])
                                                    //Remove the PrimitiveOutput (leave others as it may be connected to more)
                                                    .filter(con -> !(con.getComponent() instanceof PrimitiveOutput)),
                                            Arrays.stream(connectionsToTheOutsideWorld)).toArray(ComponentConnection[]::new);
                        }
                    }

                    //The component itself isn't connected to anything; only its constituents are.

                    componentLinks.remove(complexComponent);

                    //Remove all primitive inputs, to prevent double-connections
                    acm.getInputPins().forEach(dynamicComponentInputs::remove);
                    acm.getInputPins().forEach(componentLinks::remove);

                    //Remove primitiveOutputs connections and inputs as they won't be used (prevent pollution !)
                    //Might create null-pointing connections for when output length is not 1.
                    for (PrimitiveOutput primitiveOutput : acm.getOutputPins()) {
                        dynamicComponentInputs.remove(primitiveOutput);
                        componentLinks.remove(primitiveOutput);
                    }
                });

        //Finally, we'll set up all components to be updated next tick (the first tick) to prevent BUD states.
        //For anyone actually reading through these lines BUD is a reference to Minecraft Block Update Detection stuff, look it up ! :)
        primed = ConcurrentHashMap.newKeySet();
        primed.addAll(dynamicComponentInputs.keySet());
    }

    public void tick() {
        //Preparing for the next tick's batch.
        Set<ComponentHolder> nextPrimed = ConcurrentHashMap.newKeySet();

        //Buffer for values to write to dynamicComponentInputs, as to not introduce priority.
        ConcurrentLinkedQueue<Object[]> toSet = new ConcurrentLinkedQueue<>();

        primed.stream().parallel().forEach(toUpdate -> {
            if (!(toUpdate instanceof PrimitiveComponent)) {
                System.err.println("Non-primitive component to be ticked; Error in ACM flattening !");
                return;
            }
            //Evaluate each component and prime its destination.
            boolean[][] dynamicInputs = dynamicComponentInputs.get(toUpdate);

            //Now write the result of that into its appropriate, indexed connections.

            ComponentConnection[][] componentConnections = componentLinks.get(toUpdate);

            boolean[][] componentOutputs;

            //If component has no inputs or doesn't connect; probably a leftover from a non-1-length output connection
            if (dynamicInputs == null || componentConnections == null) {
                System.err.println("Null input or connections for component " + toUpdate.getName());
                return;
            }

            //Test for unmatching for red wire coloration
            try {
                componentOutputs = ((PrimitiveComponent) toUpdate).evaluate(dynamicInputs);
            } catch (Exception e) { //Can be unmatching wires, or IndexOutOfBounds (for wireSize too large, etc.)
                //Set wireLine to be red. In this case, the "connected" components won't get updated, at all.
                for (ComponentConnection[] connections : componentConnections) {
                    //The wireLine is shared across all connections anyway, so I can just get the first one !
                    connections[0].setState(Activation.ERROR);
                }
                return;
            }
            //Iterating through the components' outputs; components with no outputs should return an empty array when evaluated.
            for (int i = 0; i < componentOutputs.length; i++) {
                //Getting the output
                boolean[] componentOutput = componentOutputs[i];

                if (componentConnections[i] == null || componentConnections[i].length == 0) {
                    System.err.println(toUpdate.getName() + " at " + Arrays.toString(convertToTileNotation(toUpdate.getSnappedPosition()))
                            + " has null or empty connection for output " + i + ". This was probably because of a wireLine terminating only in unconnected states.");
                    continue;
                }

                //Setting wireLine of this output to green if result contains at least one true.
                //We can use [0] as wireLine is shared across every connection from a given output.
                if (componentOutput.length == 0) {
                    componentConnections[i][0].setState(Activation.FLOATING);
                } else if (any(componentOutput)) {
                    componentConnections[i][0].setState(Activation.ON);
                } else {
                    componentConnections[i][0].setState(Activation.OFF);
                }

                //Iterating through all the connections of this output
                for (ComponentConnection componentConnection : componentConnections[i]) {
                    ComponentHolder connectedComponent = componentConnection.getComponent();
                    if (connectedComponent == null) {
                        //Perhaps a dead-end, or something.
                        continue;
                    }

                    if (Arrays.equals(dynamicComponentInputs.get(connectedComponent)[componentConnection.getInPort()], componentOutput)) {
                        //Don't replace if equal !
                        continue;
                    }

                    //Prime for next tick.
                    nextPrimed.add(connectedComponent);

                    //An actual component. Set its appropriate in port to the appropriate output, but only after we've finished !
                    toSet.add(new Object[]{connectedComponent, componentConnection.getInPort(), componentOutput});
                }
            }
        });
        //Set the dynamicComponentInputs from the buffer. Streams have overhead, but for pipelined CPUs and such (in the game) this will work faster.
        toSet.stream().parallel().forEach(data -> {
            //Unsafe casting, but we're sure it's O.K. - this is just a way to neatly bundle three things together.
            boolean[] dynamicInput = dynamicComponentInputs.get((ComponentHolder) data[0])[(int) data[1]];
            boolean[] calculatedInput = (boolean[]) data[2];

            if (dynamicInput.length == calculatedInput.length) {
                System.arraycopy(calculatedInput, 0, dynamicInput, 0, calculatedInput.length);
            } else {
                dynamicComponentInputs.get((ComponentHolder) data[0])[(int) data[1]] = (boolean[]) data[2];
            }
        });

        //Set the next components to be primed from the buffer
        primed = nextPrimed;
    }

    public void floodFill(ComponentHolder root, Placeable[][] presenceMap) {
        /*
        Contract : from a component (root), look through all wired outputs and create ComponentConnections.
        Also set up an entry for it in dynamicComponentInputs.
        For all linked components, if they weren't already compiled, start a floodFill process for them too.
         */

        //Setting up entry. Wire size coherency is determined at runtime by tick().
        boolean[][] inputStorage = new boolean[root.getRelativeInputs().length][];
        for (int i = 0; i < root.getRelativeInputs().length; i++) {
            //Length 0 input is a floating state.
            boolean[] initialInput = new boolean[0];
            inputStorage[i] = initialInput;
        }
        dynamicComponentInputs.put(root, inputStorage);

        //Setting up proper array size for links. For all output, there must be a link entry; null if it's not connected.
        //Second array size is determined by followWireLine(); contains as much as there are components connected to this one output.
        componentLinks.put(root, new ComponentConnection[root.getRelativeOutputs().length][]);

        //Keep track of the current output port we're working with.
        int outputIndex = 0;
        for (int[] tileOutputPosition : getTileOutputPositions(root)) {
            if (tileOutputPosition[0] < 0 || tileOutputPosition[0] >= presenceMap.length
                    || tileOutputPosition[1] < 0 || tileOutputPosition[1] >= presenceMap[0].length) {
                //Prevent out of bounds access, component is connected to nothing.
                componentLinks.get(root)[outputIndex] = new ComponentConnection[]{new ComponentConnection(null, new ArrayList<>(), -1, true)};
                outputIndex++;
                continue;
            }

            Placeable potentialLink = presenceMap[tileOutputPosition[0]][tileOutputPosition[1]];

            //There needs to be a wire that points into the component (complement of its rotation)
            if (!(potentialLink instanceof Wire && ((Wire) potentialLink).getPartners().contains(complement(root.getRotation())))) {
                //Empty connection, iteration will skip wire color AND component thingy.
                componentLinks.get(root)[outputIndex] = new ComponentConnection[]{new ComponentConnection(null, new ArrayList<>(), -1, true)};
                outputIndex++;
                continue;
            }

            //This will return every ComponentConnection from this component, for the given outputIndex.
            ArrayList<ComponentConnection> connections = followWireLine(new ArrayList<>(), (Wire) potentialLink, root.getRotation(), presenceMap);

            //Setting this output index's componentConnections
            componentLinks.get(root)[outputIndex] = connections.toArray(ComponentConnection[]::new);

            outputIndex++;
        }
    }

    public ArrayList<ComponentConnection> followWireLine(ArrayList<Wire> wireLine, Wire root, Rotation from, Placeable[][] presenceMap) {
        //Creating list of all connections so far for this index.
        ArrayList<ComponentConnection> tally = new ArrayList<>();

        if (wireLine.contains(root)) {
            //Already visited ! So add nothing to tally. Prevent infinite loops, but if it's circular, still show it.
            //Minor pollution otherwise.
            tally.add(new ComponentConnection(null, wireLine, -1, true));
            return tally;
        }

        //Add the root of the "tree"
        wireLine.add(root);

        //Let's keep the tree spirit going. We'll have to check all of these rotations.
        Set<Rotation> branches = new HashSet<>(root.getPartners());

        //Don't go back !
        branches.remove(complement(from));

        //Only go straight for crossovers
        if (root.isCrossover()) {
            branches.remove(bendClockwise(from));
            branches.remove(complement(bendClockwise(from)));
        }

        if (branches.isEmpty()) {
            //Well, this one leads nowhere. Still need to make it graphically, but no components to update. inPort is ignored; so -1
            tally.add(new ComponentConnection(null, wireLine, -1, true));
            return tally;
        }

        for (Rotation checkDirection : branches) {
            /*
            For branching off, or connecting components.
            It's impossible to have something pointing into void here, as the only time that happens is with sub-wires (only 1 direction, removed)
             */

            int[] tilePosition = convertToTileNotation(root.getSnappedPosition());
            int[] checkTilePosition = getDirectionalLocation(tilePosition, checkDirection, 1);

            //A wire can't point to outside the map, so this should never throw an error. Keyword : should !
            Placeable potentialMatch = presenceMap[checkTilePosition[0]][checkTilePosition[1]];

            if (potentialMatch instanceof Wire) {
                //We know it's reciprocating, as it would get culled otherwise.
                tally.addAll(followWireLine(wireLine, (Wire) potentialMatch, checkDirection, presenceMap));
                continue;
            }

            //We know it's a componentHolder now; except for leftover crossovers pointing into air.
            if (potentialMatch == null) {
                if (root.isCrossover()) {
                    //Include the crossover !
                    tally.add(new ComponentConnection(null, wireLine, -1, true));
                } else {
                    System.err.println("Warning: wire pointing to air (illegal state) on ACM creation at " + Arrays.toString(checkTilePosition));
                }
                continue;
            }

            ComponentHolder connectedComponent = (ComponentHolder) potentialMatch;

            //Get the input index where we're pointing through a nifty iteration of the component's input locations.
            int inputIndex = 0;
            boolean success = false;
            for (int[] potentialWireTilePosition : getTileInputPositions(connectedComponent)) {
                //Iterate through and find which input matches the wire.
                if (Arrays.equals(potentialWireTilePosition, tilePosition)){
                    success = true;
                    break;
                }
                inputIndex++;
            }

            if (!success) {
                //Either a wire pointing into a component but no an IO and no stubs to create an appropriate connection, or a wire with no stubs (no connection !)
                System.err.println("Failed to get an input position match following wireLine on ACM creation for component " + connectedComponent.getName() + ". " +
                        "This was probably caused by an illegal doubly-written-to wire, or a wire pointing into a component, but not to an input/output.");

                //Still add connection to light it up (and prevent some cases of null conections)
                tally.add(new ComponentConnection(null, wireLine, -1, true));
                continue;
            }

            if (connectedComponent instanceof PrimitiveComponent) {
                tally.add(new ComponentConnection(connectedComponent, wireLine, inputIndex, true));
            } else {
                //Use the connection of the primitiveInput represented by this index (
                AbstractComponentMap complexComponentACM = connectedComponent.getAcm();
                ComponentHolder primitiveInput = complexComponentACM.getInputPins().get(inputIndex);
                ComponentConnection[][] inputConnections = complexComponentACM.getComponentLinks().get(primitiveInput);

                //Input only has one output. Still, a small test here in case (somehow) a bug gets introduced, otherwise it'd be hard to track it down.
                if (inputConnections.length != 1) {
                    System.err.println("Error : PrimitiveInput connection length is not 1 in " + connectedComponent.getName() + " at " + inputIndex + ".");
                    continue;
                }

                //Get all connections to redirect.
                ComponentConnection[] connections = inputConnections[0];
                for (ComponentConnection connection : connections) {
                    //If the connection is to a PrimitiveOutput, then shortcut straight to the appropriate output!
                    connection.setTopLevel(false);
                    ComponentHolder component = connection.getComponent();
                    if (component instanceof PrimitiveOutput) {
                        //Instead continue the wireLine as if it was exiting the component
                        int outputIndex = complexComponentACM.getOutputPins().indexOf(component);
                        int[] exitingWirePosition = getTileOutputPositions(connectedComponent)[outputIndex];
                        Placeable placeable;
                        try {
                            placeable = presenceMap[exitingWirePosition[0]][exitingWirePosition[1]];
                        } catch (Exception e) {
                            //Probably placed too close to the bounds; tried to access out of bounds
                            placeable = null;
                        }

                        if (placeable instanceof Wire) {
                            //If not instanceOf wire, the fallback visual default input wireLine will still exist!
                            tally.addAll(followWireLine(wireLine, (Wire) placeable, connectedComponent.getRotation(), presenceMap));
                        }
                    } else {
                        tally.add(connection);
                    }
                }
                //Add the connection to light up the wire on the way to the complex component (purely visual). First because setState takes the first element !
                tally.addFirst(new ComponentConnection(null, wireLine, -1, true));
            }
        }
        return tally;
    }

    public Set<ComponentHolder> getPrimed() {
        return primed;
    }

    public void prime(ComponentHolder toPrime) {
        primed.add(toPrime);
    }

    public ArrayList<PrimitiveInput> getInputPins() {
        return inputPins;
    }

    public ArrayList<PrimitiveOutput> getOutputPins() {
        return outputPins;
    }

    public ConcurrentHashMap<ComponentHolder, boolean[][]> getDynamicComponentInputs() {
        return dynamicComponentInputs;
    }

    public ConcurrentHashMap<ComponentHolder, ComponentConnection[][]> getComponentLinks() {
        return componentLinks;
    }
}