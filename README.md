# Distributed Collaborative Puzzle

*Authors:
[Paolo Baldini](https://github.com/Mandrab),
[Ylenia Battistini](https://github.com/yleniaBattistini)*

The project, developed for the *Concurrent and Distributed Programming* course at University of Bologna,
is about the development of a multiplayer service-based puzzle game.

![Render of a game](res/multiplayer_puzzle.gif)

## Usage
Before launch the application through the `Main.kt` class, you need to start a fake server.
The service uses it both for the (fake) db as for store puzzle tiles.<br>
On **linux** systems (and I think also on the **macOS** ones) you can use this script to set up the whole environment:
`init.sh`<br>
On **windows** ones, create a `trash` folder in the project root and start a server on it.
The python script in `init.sh`, used to launch the server, should work fine.