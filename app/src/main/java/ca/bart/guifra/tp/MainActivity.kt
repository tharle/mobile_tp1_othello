package ca.bart.guifra.tp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.core.util.toAndroidXPair
import androidx.core.view.children
import ca.bart.guifra.tp.databinding.ActivityMainBinding


data class Cell(var pressed: Boolean = false)

data class Player(var discId: Int = R.drawable.filled_disc_player_0, var score: Int = 0 ,var isIA: Boolean = false)

data class Model(
    val grid: Array<Cell> = Array(9) { Cell() },
    val players: Array<Player> = Array(4){Player()},
    val currentPlayerIndex: Int = 0
)

class MainActivity : Activity() {

    companion object {

        const val TAG = "MainActivity"

        const val NB_COLUMNS = 3
        const val NB_ROWS = 3

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
    }

    fun onButtonClicked(index:Int) {


        val coordinates:Pair<Int, Int> = index.toCoordinates()

        Log.d(TAG, "onButtonClicked($index) = $coordinates")
        Log.d(TAG, "coordinates.x = ${coordinates.x}")
        Log.d(TAG, "coordinates.y = ${coordinates.y}")
        Log.d(TAG, "coordinates.toIndex() = ${coordinates.toIndex()}")




        model.grid[index + (NORTH + EAST)]?.pressed = true



        // is this a valid move?

        // if yes, DO THE MOVE

        // player++ <--- new valid move list

        model.grid[index].pressed = true
        refresh()
    }

    fun GetCurrentPlayer(): Player {
        return model.players[model.currentPlayerIndex];
    }

    fun refresh() {

        val currentPlayer = GetCurrentPlayer();
        // update display of current player
        binding.turnIcon.setBackgroundResource(currentPlayer.discId)

        // update player corners (score, state of skip button)
        refreshPlayer0();


        model.grid.asSequence().zip(binding.grid.children)
            .forEach { (cell, button) ->

                // show valid moves (from model?)

                button.setBackgroundResource(
                    // probablement un "when" pour vous..
                    if (cell.pressed) R.drawable.filled_disc
                    else R.drawable.empty_disc
                )
            }
    }

    fun refreshPlayer0() {
        val player = model.players[0]
        binding.player0Icon.setBackgroundResource(player.discId)
        binding.player0Score.text = player.score.toString()
        binding.player0Button.isEnabled = model.currentPlayerIndex == 0
    } // TODO faire ça
}


private fun Int.toCoordinates() = Pair(this % MainActivity.NB_COLUMNS, this / MainActivity.NB_COLUMNS)
private val Pair<Int, Int>.x get() = first
private val Pair<Int, Int>.y get() = second
private fun Pair<Int, Int>.toIndex() = y * MainActivity.NB_COLUMNS + x
private operator fun Pair<Int, Int>.plus(other:Pair<Int, Int>) = Pair(this.x + other.x, this.y + other.y)
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
