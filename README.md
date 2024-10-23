# SCS
A simple digital circuit simulator.

Controls as of version 0.3 :

(Mouse)
Left click to place held component, or to interact with a PrimitiveInput.
Middle click + drag OR shift + left click + drag to lay down a wire line.
Right click to delete a wire or component.
Right click + drag to delete a line of wires. (Must start on an empty tile)
Middle click on a component to pipette it. (Copy rotation and kind)
Left click on an InteractiveTextField (Options and Saving) to select it.

(Keyboard)
'r' to change rotations (shift to go backwards)
'e' to toggle inventory.
'o' to toggle options menu.
'CTRL+S' to open saving menu.
'ESC' to go back to building mode.

(Inventory)
Left click to choose a component, or go inside a directory (marked in yellow).
Right click a custom component to edit it (save changes by overwriting in saving menu).
Right click a primitive component to clear board.
Manage inventory and create directories with a file explorer.

(Options)
Base component wire size affects primitives such as True, False, and Input.

Splitter and merger intervals are separated by '_' and are always inclusive. Backwards intervals act as in-order intervals.
Example : a splitter splitting an 8-bit bus into two 4-bit busses (0 to 3 and 4 to 7) is written as 0-3_4-7.
Shorthand for n-n is simply n. Splitting a 4-bit bus into all its bits would simply be : 0_1_2_3

Updates per second are precise to the UPS until 1000, at which point it will be truncated to the nearest thousand. (1800 = 1000, 2900 = 2000)

(Saving)
Relative I/O is relative to the bottom-left of the component, when it is facing north.
Example : a primitive AND gate has relative outputs {1} and relative inputs {0, 2}.
Saving a component will also clear the board. To retrieve it, simply go to edit it through the inventory.
Left click on the beautiful orange SAVE button to save.
