package me.analyzers.scs.game;

import me.analyzers.scs.primitiveComponents.*;
import me.analyzers.scs.utilities.Activation;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.*;

import static me.analyzers.scs.utilities.MathUtils.*;

public class AbstractComponentMap {
    //Holds the current ACM's component inputs.
    private final HashMap<ComponentHolder, boolean[][]> dynamicComponentInputs;

    //Holds all connections for every compiled component, by its output index.
    private final HashMap<ComponentHolder, ComponentConnection[][]> componentLinks;

    //Holds all ComponentHolders which must be updated next tick
    private LinkedHashSet<ComponentHolder> primed;

    //Ordered lists of I/O. Required for mapping an ACM another as a complex component (hookup is done through these)
    //"Components" without I/O pins shouldn't even be able to connect to components (no pins!)
    private final ArrayList<PrimitiveInput> inputPins = new ArrayList<>();
    private final ArrayList<PrimitiveOutput> outputPins = new ArrayList<>();

    public AbstractComponentMap(Placeable[][] presenceMap, List<ComponentHolder> componentQueue) {
        /*
        The contract is such:
        guarantee all ACM state fields are properly setup.
        Flatten any complex components into this ACM, merging their ACM, omitting their in/out pins (unnecessary penalty)
        Display proper wire colors for toplevel wireLines.
         */

        //First, form I/O pins.
        for (Placeable[] row : presenceMap) {
            for (Placeable col : row) {
                if (col instanceof PrimitiveInput) {
                    inputPins.add((PrimitiveInput) col);
                } else if (col instanceof PrimitiveOutput) {
                    outputPins.add((PrimitiveOutput) col);
                }
            }
        }

        //Now, for each componentHolder, we'll begin a flood-fill that creates link and sets up their inputs.
        dynamicComponentInputs = new HashMap<>();
        componentLinks = new HashMap<>();

        for (ComponentHolder root : componentQueue) {
            if (!dynamicComponentInputs.containsKey(root)) {
                floodFill(root, presenceMap);
            }
        }

        //Finally, we'll set up all components to be updated next tick (the first tick) to prevent BUD states.
        //For anyone actually reading through these lines BUD is a reference to Minecraft Block Update Detection stuff, look it up ! :)
        primed = new LinkedHashSet<>(dynamicComponentInputs.keySet());

        /*
        System.out.println("DEBUG : dynamicComponentInputs " + dynamicComponentInputs);
        System.out.println("DEBUG : componentLinks " + componentLinks);
        */

        //(TODO) Now, insert the ACM of complex components while keeping the proper connections (remove primitive I/O)
    }

    public void tick() {
        //Preparing for the next tick's batch.
        LinkedHashSet<ComponentHolder> nextPrimed = new LinkedHashSet<>();

        //Buffer for values to write to dynamicComponentInputs, as to not induce priority.
        ArrayList<Object[]> toSet = new ArrayList<>();

        for (ComponentHolder toUpdate : primed) {
            if (!(toUpdate instanceof PrimitiveComponent)) {
                System.err.println("Non-primitive component to be ticked; Error in ACM flattening !");
                continue;
            }
            //Evaluate each component and prime its destination.
            boolean[][] dynamicInputs = dynamicComponentInputs.get(toUpdate);

            //Now write the result of that into its appropriate, indexed connections.

            ComponentConnection[][] componentConnections = componentLinks.get(toUpdate);

            boolean[][] componentOutputs;

            //Test for unmatching for red wire coloration
            try {
                componentOutputs = ((PrimitiveComponent) toUpdate).evaluate(dynamicInputs);
            } catch (Exception e) { //Can be unmatching wires, or IndexOutOfBounds (for wireSize too large, etc.)

                //Set wireLine to be red. In this case, the "connected" components won't get updated, at all.
                for (ComponentConnection[] connections : componentConnections) {
                    //The wireLine is shared across all connections anyway, so I can just get the first one !
                    connections[0].getWireLine().forEach(w -> w.setState(Activation.ERROR));
                }
                continue;
            }


            //Iterating through the components' outputs
            for (int i = 0; i < componentOutputs.length; i++) {
                //Getting the output
                boolean[] componentOutput = componentOutputs[i];

                if (componentConnections[i] == null || componentConnections[i].length == 0) {
                    System.err.println(toUpdate.getName() + " at " + Arrays.toString(convertToTileNotation(toUpdate.getSnappedPosition()))
                            + " has null or empty connection for output " + i + ". This was probably caused by a wire pointing into a component, but not to an input/output.");
                    continue;
                }

                //Setting wireLine of this output to green if result contains at least one true.
                //We can use [0] as wireLine is shared across every connection from a given output.
                //TODO : does this impact performance? Should check for topLevel ?
                if (any(componentOutput)) {
                    componentConnections[i][0].getWireLine().forEach(w -> w.setState(Activation.ON));
                } else {
                    componentConnections[i][0].getWireLine().forEach(w -> w.setState(Activation.OFF));
                }

                //Iterating through all the connections of this output
                for (ComponentConnection componentConnection : componentConnections[i]) {

                    ComponentHolder connectedComponent = componentConnection.getComponent();
                    if (connectedComponent == null) {
                        //Perhaps a dead-end, or something.
                        continue;
                    }

                    //Prime for next tick.
                    nextPrimed.add(connectedComponent);

                    //An actual component. Set its appropriate in port to the appropriate output, but only after we've finished !
                    toSet.add(new Object[]{connectedComponent, componentConnection.getInPort(), componentOutput});
                }
            }
        }

        //Set the dynamicComponentInputs from the buffer
        for (Object[] data : toSet) {
            //Unsafe casting, but we're sure it's O.K. - this is just a way to neatly bundle three things together.
            dynamicComponentInputs.get((ComponentHolder) data[0])[(int) data[1]] = (boolean[]) data[2];
        }

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
            //TODO : fix here to force merger to have size 1 inputs. For modular/complex mergers, this part of the code might need a redo.
            boolean[] initialInput = new boolean[root instanceof PrimitiveMerger ? 1 : root.getWireSize()];
            Arrays.fill(initialInput, false);
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

        //Start a floodFill process for all non-processed componentHolders connected to this one
        for (ComponentConnection[] connections : componentLinks.get(root)) {
            for (ComponentConnection connection : connections) {
                if (dynamicComponentInputs.containsKey(connection.getComponent())) {
                    continue;
                }

                //Might be a wire leading nowhere.
                if (connection.getComponent() == null) {
                    continue;
                }

                //Recurse !
                floodFill(connection.getComponent(), presenceMap);
            }
        }
    }

    public ArrayList<ComponentConnection> followWireLine(ArrayList<Wire> wireLine, Wire root, Rotation from, Placeable[][] presenceMap) {
        //Add the root of the "tree"
        if (wireLine.contains(root)) {
            //Already visited ! So add nothing to tally. Prevent infinite loops.
            return new ArrayList<>();
        }

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

        //Creating list of all connections so far for this index.
        ArrayList<ComponentConnection> tally = new ArrayList<>();

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
            if (success) {
                tally.add(new ComponentConnection(connectedComponent, wireLine, inputIndex, true));
            } else {
                System.err.println("Failed to get an input position match following wireLine on ACM creation. " +
                        "This was probably caused by an illegal doubly-written-to wire, or a wire pointing into a component, but not to an input/output.");
            }
        }
        return tally;
    }

    public void primeAll() {
        primed.addAll(dynamicComponentInputs.keySet());
    }

    public ArrayList<PrimitiveInput> getInputPins() {
        return inputPins;
    }

    public ArrayList<PrimitiveOutput> getOutputPins() {
        return outputPins;
    }
}