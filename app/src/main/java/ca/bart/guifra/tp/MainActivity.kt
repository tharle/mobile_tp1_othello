package ca.bart.guifra.tp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.core.view.children
import ca.bart.guifra.tp.databinding.ActivityMainBinding


data class Cell(var idPlayer: Int = -1)

data class Player(
    var idPlayer: Int,
    var idDisc: Int = R.drawable.filled_disc,
    var score: Int = 0,
    var isIA: Boolean = false
)

data class Model(
    val grid: Array<Cell> = Array(64) { Cell() },
    val validIdCells: ArrayList<Int> = arrayListOf<Int>(),
    val players: Array<Player> = arrayOf(
        Player(0, R.drawable.filled_disc_player_0),
        Player(1, R.drawable.filled_disc_player_1),
        Player(2, R.drawable.filled_disc_player_2),
        Player(3, R.drawable.filled_disc_player_3)
    ),
    var currentIdPlayer: Int = 0
)

class MainActivity : Activity() {

    companion object {

        const val TAG = "MainActivity"

        const val NB_COLUMNS = 8
        const val NB_ROWS = 8
        const val ID_PLAYER_EMPTY = -1
        const val ID_PLAYER_VALID = 666
        val ID_DISC_EMPTY = R.drawable.empty_disc
        val ID_DISC_VALID = R.drawable.filled_disc

        val NORTH = Pair(0, -1)
        val WEST = Pair(-1, 0)
        val EAST = Pair(1, 0)
        val SOUTH = Pair(0, 1)


    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    val model = Model()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.grid.children.forEachIndexed { index, button ->
            button.setOnClickListener {
                onButtonClicked(index)
            }
        }

        binding.player0Button.setOnClickListener({ nextPlayer() })
        binding.player1Button.setOnClickListener({ nextPlayer() })
        binding.player2Button.setOnClickListener({ nextPlayer() })
        binding.player3Button.setOnClickListener({ nextPlayer() })

        initGrid()
    }

    fun initGrid() {

        model.grid.forEach {
            it.idPlayer = ID_PLAYER_EMPTY
        }
        model.grid[27].idPlayer = 0
        model.grid[28].idPlayer = 1
        model.grid[35].idPlayer = 3
        model.grid[36].idPlayer = 2

        calculValidsCells()
        refresh()
    }

    fun onButtonClicked(index: Int) {
        val coordinates: Pair<Int, Int> = index.toCoordinates()

        Log.d(TAG, "onButtonClicked($index) = $coordinates")
        Log.d(TAG, "coordinates.x = ${coordinates.x}")
        Log.d(TAG, "coordinates.y = ${coordinates.y}")
        Log.d(TAG, "coordinates.toIndex() = ${coordinates.toIndex()}")

        // is this a valid move?
        if (!model.validIdCells.contains(index)) return;


        // if yes, DO THE MOVE
        model.grid[index].idPlayer = model.currentIdPlayer

        // Get all from the direction
        //model.grid[index + (NORTH + EAST)]?.idPlayer = model.currentIdPlayer


        // player++ <--- new valid move list
        nextPlayer();
    }

    fun nextPlayer() {
        model.currentIdPlayer++
        model.currentIdPlayer =
            if (model.currentIdPlayer < model.players.size) model.currentIdPlayer else 0

        calculValidsCells()

        refresh()
    }

    fun addValidCell(index: Int) {
        model.validIdCells.add(index)
        model.grid[index].idPlayer = ID_PLAYER_VALID
    }

    fun calculValidsCells() {
        cleanCellsValids()

        val idPlayer = model.currentIdPlayer

        var index = 0
        model.grid.forEach {
            if (it.idPlayer == idPlayer) {

                //val direction = SOUTH

                // Check all valids
                for(x in -1 .. 1) {
                    for(y in -1 .. 1) {
                        val direction = Pair(x, y)

                        if(direction.x == 0 && direction.y == 0) continue

                        val idsCellEnemiesFound: ArrayList<Int> = ArrayList()
                        var coordinates: Pair<Int, Int> = index.toCoordinates().plus(direction)
                        while (
                            (direction.x != 0 && coordinates.x in 0 until NB_COLUMNS)
                            || (direction.y != 0 && coordinates.y in 0 until NB_ROWS)) {
                            val idCell = coordinates.toIndex()
                            val playerLoop = model.grid[idCell]

                            if(playerLoop.idPlayer == ID_PLAYER_EMPTY) {
                                if(idsCellEnemiesFound.isNotEmpty()) addValidCell(idCell)
                                break // if found empty space, end it
                            }  else if(playerLoop.idPlayer == idPlayer) {
                                idsCellEnemiesFound.clear()  // if same id, restart list of cells to convert
                            }
                            else {
                                idsCellEnemiesFound.add(idCell) // add to list to convert all
                            }

                            coordinates = coordinates.plus(direction)
                        }
                    }
                }

                //Converts all we found in our way to empty space
                //idCellsToConvert.forEach{idCell -> model.grid[idCell]?.idPlayer = idPlayer}

            }

            index++
        }
    }

    fun cleanCellsValids() {
        model.validIdCells.clear()
        model.grid
            .filter { cell -> cell.idPlayer == ID_PLAYER_VALID }
            .forEach { cell -> cell.idPlayer = ID_PLAYER_EMPTY }
    }

    fun GetCurrentPlayer(): Player {
        return model.players[model.currentIdPlayer];
    }

    fun refresh() {

        val currentPlayer = GetCurrentPlayer();
        // update display of current player
        binding.turnIcon.setBackgroundResource(currentPlayer.idDisc)

        // update player corners (score, state of skip button)
        refreshPlayers();

        model.grid.asSequence().zip(binding.grid.children)
            .forEach { (cell, button) ->

                // show valid moves (from model?)

                button.setBackgroundResource(
                    when (cell.idPlayer) {
                        ID_PLAYER_EMPTY -> ID_DISC_EMPTY
                        ID_PLAYER_VALID -> ID_DISC_VALID
                        else -> model.players[cell.idPlayer].idDisc
                    }
                )
            }
    }

    fun refreshPlayers() {
        var player: Player = model.players[0]
        binding.player0Icon.setBackgroundResource(player.idDisc)
        binding.player0Score.text = player.score.toString()
        binding.player0Button.isEnabled = model.currentIdPlayer == player.idPlayer

        player = model.players[1]
        binding.player1Icon.setBackgroundResource(player.idDisc)
        binding.player1Score.text = player.score.toString()
        binding.player1Button.isEnabled = model.currentIdPlayer == player.idPlayer

        player = model.players[2]
        binding.player2Icon.setBackgroundResource(player.idDisc)
        binding.player2Score.text = player.score.toString()
        binding.player2Button.isEnabled = model.currentIdPlayer == player.idPlayer

        player = model.players[3]
        binding.player3Icon.setBackgroundResource(player.idDisc)
        binding.player3Score.text = player.score.toString()
        binding.player3Button.isEnabled = model.currentIdPlayer == player.idPlayer
    }
}


private fun Int.toCoordinates() =
    Pair(this % MainActivity.NB_COLUMNS, this / MainActivity.NB_COLUMNS)

private val Pair<Int, Int>.x get() = first
private val Pair<Int, Int>.y get() = second
private fun Pair<Int, Int>.toIndex() : Int {
    val campX = x.coerceIn(0, MainActivity.NB_COLUMNS - 1)
    val campY = y.coerceIn(0, MainActivity.NB_ROWS - 1)
    return campY * MainActivity.NB_COLUMNS + campX
}
private operator fun Pair<Int, Int>.plus(other: Pair<Int, Int>) =
    Pair(this.x + other.x, this.y + other.y)

private operator fun Int.plus(direction: Pair<Int, Int>): Int? =
    (toCoordinates() + direction).let { asCoordinates ->

        if ((0 until MainActivity.NB_COLUMNS).contains(asCoordinates.x) &&
            (0 until MainActivity.NB_ROWS).contains(asCoordinates.y)
        )
            asCoordinates.toIndex()
        else
            null
    }

/*
{
    val asCoordinates = toCoordinates() + direction
    return if ((0 until MainActivity.NB_COLUMNS).contains(asCoordinates.x) &&
        (0 until MainActivity.NB_ROWS).contains(asCoordinates.y))
        asCoordinates.toIndex()
    else
        null
}
*/

private operator fun <T> Array<T>.get(i: Int?): T? {
    i ?: return null
    return this[i]
}
