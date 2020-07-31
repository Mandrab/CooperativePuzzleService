# Distributed Collaborative Puzzle

*Authors:
[Paolo Baldini](https://github.com/Mandrab),
[Ylenia Battistini](https://github.com/yleniaBattistini)*

The project, developed for the *Concurrent and Distributed Programming* course at University of Bologna, is about the
 development of a multiplayer service-based puzzle game.

![Render of a game](res/multiplayer_puzzle.gif)

## The game
This is a multiplayer distributed version of a puzzle game. Players can join to a game using the API (no need of
 registration) and can move the tiles with the goal of complete the puzzle. To emphasize the multiplayer
 factor of the game, it has been chosen to make other players mouses are visible.

## The service
This game is developed as a service and allow use by a variety of client systems. This is indeed a RESTful service and
 thus client is fully replaceable.<br>
Beside the classic API however, also a *web socket* support is provided. This one allows clients to send mouse
 positions and visualize other players ones in the game in almost real-time. Without use that way, only polling a
 request could allow the same effect.

## Usage
Before launch the application through the `Main.kt` class, you need to start a fake server.
The service uses it both for the (fake) db as for store puzzle tiles.<br>
On **linux** systems (and I think also on the **macOS** ones) you can use the `init.sh` 
script to set up the whole environment.<br>
On **windows** ones, create a `trash` folder in the project root and start a server on this
latter one.
The python script in `init.sh`, used to launch the server, should work fine also under windows.