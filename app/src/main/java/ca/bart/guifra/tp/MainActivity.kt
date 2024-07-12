package ca.bart.guifra.tp

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.CheckBox
import androidx.core.view.children
import ca.bart.guifra.tp.databinding.ActivityMainBinding
import kotlinx.parcelize.Parcelize
import kotlin.random.Random


@Parcelize
data class Cell(var idCell: Int, var idPlayer: Int = -1) : Parcelable

@Parcelize
data class Player(
    var idPlayer: Int,
    var idDisc: Int = R.drawable.filled_disc,
    var score: Int = 0,
    var idRevenge: Int = MainActivity.ID_PLAYER_EMPTY
) : Parcelable

@Parcelize
data class Model(
    val grid: Array<Cell> = Array(64) { Cell(it) },
    val validIdCells: ArrayList<Int> = arrayListOf<Int>(),
    val players: Array<Player> = arrayOf(
        Player(0, R.drawable.filled_disc_player_0),
        Player(1, R.drawable.filled_disc_player_1),
        Player(2, R.drawable.filled_disc_player_2),
        Player(3, R.drawable.filled_disc_player_3)
    ),
    var currentIdPlayer: Int = 0
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Model

        if (!grid.contentEquals(other.grid)) return false
        if (validIdCells != other.validIdCells) return false
        if (!players.contentEquals(other.players)) return false
        if (currentIdPlayer != other.currentIdPlayer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentHashCode()
        result = 31 * result + validIdCells.hashCode()
        result = 31 * result + players.contentHashCode()
        result = 31 * result + currentIdPlayer
        return result
    }
}

class MainActivity : Activity() {

    companion object {

        const val TAG = "MainActivity"

        const val NB_COLUMNS = 8
        const val NB_ROWS = 8
        const val ID_PLAYER_EMPTY = -1
        const val ID_PLAYER_VALID = 666
        const val KEY_MODEL = "KEY_MODEL"
        val ID_DISC_EMPTY = R.drawable.empty_disc
        val ID_DISC_VALID = R.drawable.filled_disc


        val NORTH = Pair(0, -1)
        val WEST = Pair(-1, 0)
        val EAST = Pair(1, 0)
        val SOUTH = Pair(0, 1)
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    var model = Model()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.grid.children.forEachIndexed { index, button ->
            button.setOnClickListener {
                onButtonGridClicked(index)
            }
        }

        binding.player0Button.setOnClickListener { onButtonPlayerClicked(binding.player0CbIA.isChecked) }
        binding.player1Button.setOnClickListener { onButtonPlayerClicked(binding.player1CbIA.isChecked) }
        binding.player2Button.setOnClickListener { onButtonPlayerClicked(binding.player2CbIA.isChecked) }
        binding.player3Button.setOnClickListener { onButtonPlayerClicked(binding.player3CbIA.isChecked) }

        binding.player0CbIA.setOnClickListener { toogleIAButton(0, it) }
        binding.player1CbIA.setOnClickListener { toogleIAButton(1, it) }
        binding.player2CbIA.setOnClickListener { toogleIAButton(2, it) }
        binding.player3CbIA.setOnClickListener { toogleIAButton(3, it) }

        initGrid()
    }

    private fun initGrid() {

        model.grid.forEach {
            it.idPlayer = ID_PLAYER_EMPTY
        }

        model.grid[27].idPlayer = 0
        model.grid[28].idPlayer = 1
        model.grid[35].idPlayer = 3
        model.grid[36].idPlayer = 2
        model.players.forEach { player -> player.score = 1 }
        checkAndAddAllValidsCells()
        refresh()
    }


    private fun toogleIAButton(idPlayer: Int, button: View) {
        if(button !is CheckBox) return;

        val checkBox:CheckBox = button;

        val buttonText = getString(if(checkBox.isChecked) R.string.play else R.string.pass)

        when(idPlayer){
            0 -> {
                binding.player0Button.text = buttonText
            }
            1 -> {
                binding.player1Button.text = buttonText
            }
            2 -> {
                binding.player2Button.text = buttonText
            }
            3 -> {
                binding.player3Button.text = buttonText
            }
        }
    }

    private fun onButtonPlayerClicked(isIACheckd: Boolean) {
        if(isIACheckd) doIAChoise();
        else nextPlayer()
    }


    private fun onButtonGridClicked(index: Int) {
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
        convertirAllCell(coordinates)

        //update all scores
        updateScores()

        // player++ <--- new valid move list
        nextPlayer();
    }

    private fun updateScores(){
        model.players.forEach { player ->
            player.score = 0
            player.idRevenge = if(player.idRevenge == ID_PLAYER_EMPTY) model.currentIdPlayer else player.idRevenge
        }

        model.grid
            .filter { cell  -> cell.idPlayer != ID_PLAYER_VALID && cell.idPlayer != ID_PLAYER_EMPTY }
            .forEach { cell ->
                model.players[cell.idPlayer].score++
                model.players[cell.idPlayer].idRevenge = ID_PLAYER_EMPTY
            }
    }

    private fun nextPlayer() {
        model.currentIdPlayer++
        model.currentIdPlayer =
            if (model.currentIdPlayer < model.players.size) model.currentIdPlayer else 0

        checkAndAddAllValidsCells()

        refresh()
    }

    private fun doIAChoise() {

        if(model.validIdCells.isEmpty())
        {
            nextPlayer()
            return
        }

        // IA will aways try to get the conner
        var idCellsFound : List<Int> = model.validIdCells
            .filter { index : Int ->
                val coordinates = index.toCoordinates()
                coordinates.x == 0 || coordinates.x == (NB_COLUMNS - 1) || coordinates.y == 0 || coordinates.y == (NB_ROWS - 1) }

        if(idCellsFound.isEmpty()) idCellsFound = model.validIdCells
        val indexMax = idCellsFound.size - 1
        if(indexMax < 0) return

        val indexIA = if(indexMax > 0) Random.nextInt(indexMax) else 0
        onButtonGridClicked(idCellsFound[indexIA])

    }

    private fun addValidCell(index: Int) {
        model.validIdCells.add(index)
        model.grid[index].idPlayer = ID_PLAYER_VALID
    }

    //Check and add all valids cells for current player
    private fun checkAndAddAllValidsCells() {
        cleanCellsValids()
        val currentPlayer = getCurrentPlayer()

        val idPlayer =
            if(currentPlayer.score <= 0 && currentPlayer.idRevenge != ID_PLAYER_EMPTY) currentPlayer.idRevenge
            else currentPlayer.idPlayer


        model.grid.forEach {cell ->
            if (cell.idPlayer == idPlayer) {

                // Check all valids in all 8 directions
                for(x in WEST.x .. EAST.x) {
                    for(y in NORTH.y .. SOUTH.y) {
                        val direction = Pair(x, y)

                        if(direction.x == 0 && direction.y == 0) continue // Ignore 0, 0 because its not a direction

                        val idsCellEnemiesFound: ArrayList<Int> = ArrayList()
                        var coordinates: Pair<Int, Int> = cell.idCell.toCoordinates().plus(direction)
                        while (
                            (direction.x != 0 && coordinates.x in 0 until NB_COLUMNS)
                            || (direction.y != 0 && coordinates.y in 0 until NB_ROWS)) {
                            val idCell = coordinates.toIndex()
                            val playerLoop = model.grid[idCell]

                            when(playerLoop.idPlayer){
                                ID_PLAYER_EMPTY -> {
                                    if(idsCellEnemiesFound.isNotEmpty()) addValidCell(idCell)
                                    break // if found empty space, end it
                                }
                                // if found one valid, just jump for next check
                                ID_PLAYER_VALID -> break
                                // if same id, restart list of cells to convert
                                idPlayer        -> idsCellEnemiesFound.clear()
                                // add to list to convert all
                                else            -> idsCellEnemiesFound.add(idCell)
                            }

                            coordinates = coordinates.plus(direction)
                        }
                    }
                }

            }
        }
    }

    private fun convertirAllCell(startPosition: Pair<Int, Int>) {

        val allIdCellsToConvert: ArrayList<Int> = ArrayList()
        val player = getCurrentPlayer()
        val idPlayerTarget = if(player.idRevenge == ID_PLAYER_EMPTY) player.idPlayer else player.idRevenge
        model.grid
            .filter { cell -> cell.idPlayer == idPlayerTarget }
            .forEach {cell ->
                // Check all valids
                for(x in WEST.x .. EAST.x) {
                    for(y in NORTH.y .. SOUTH.y) {
                        val direction = Pair(x, y)

                        if(direction.x == 0 && direction.y == 0) continue

                        val idCellsToConvert: ArrayList<Int> = ArrayList()
                        var coordinate: Pair<Int, Int> = startPosition.plus(direction)
                        while (
                            (direction.x != 0 && coordinate.x in 0 until NB_COLUMNS)
                            || (direction.y != 0 && coordinate.y in 0 until NB_ROWS)) {
                            val idCell = coordinate.toIndex()
                            val playerLoop = model.grid[idCell]

                            when(playerLoop.idPlayer){
                                ID_PLAYER_EMPTY -> break // if found empty space, end it
                                // if found one valid, just jump for next check
                                ID_PLAYER_VALID -> break
                                // if same id, restart list of cells to convert
                                idPlayerTarget  -> {
                                    allIdCellsToConvert.addAll(idCellsToConvert)
                                    break
                                }
                                // add to list to convert all
                                else            -> idCellsToConvert.add(idCell)
                            }

                            coordinate = coordinate.plus(direction)
                        }
                    }
                }
        }

        //Converts all we found in our way to empty space
        allIdCellsToConvert.forEach{idCell -> model.grid[idCell]?.idPlayer = player.idPlayer}
    }

    private fun cleanCellsValids() {
        model.validIdCells.clear()
        model.grid
            .filter { cell -> cell.idPlayer == ID_PLAYER_VALID }
            .forEach { cell -> cell.idPlayer = ID_PLAYER_EMPTY }
    }

    private fun getCurrentPlayer(): Player {
        return model.players[model.currentIdPlayer];
    }

    private fun refresh() {

        val currentPlayer = getCurrentPlayer();
        // update display of current player
        binding.turnIcon.setBackgroundResource(currentPlayer.idDisc)

        if(currentPlayer.idRevenge != ID_PLAYER_EMPTY) {
            binding.turnRevengeIcon.setBackgroundResource(model.players[currentPlayer.idRevenge].idDisc)
            binding.turnRevengeIcon.visibility = View.VISIBLE
            binding.turnRevengeSeparator.visibility = View.VISIBLE
        } else {
            binding.turnRevengeIcon.visibility = View.GONE
            binding.turnRevengeSeparator.visibility = View.GONE
        }

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

    private fun refreshPlayers() {
        var player: Player = model.players[0]
        binding.player0Icon.setBackgroundResource(player.idDisc)
        binding.player0Score.text = player.score.toString()
        binding.player0Button.isEnabled = model.currentIdPlayer == player.idPlayer
        binding.player0CbIA.isEnabled = model.currentIdPlayer == player.idPlayer

        player = model.players[1]
        binding.player1Icon.setBackgroundResource(player.idDisc)
        binding.player1Score.text = player.score.toString()
        binding.player1Button.isEnabled = model.currentIdPlayer == player.idPlayer
        binding.player1CbIA.isEnabled = model.currentIdPlayer == player.idPlayer

        player = model.players[2]
        binding.player2Icon.setBackgroundResource(player.idDisc)
        binding.player2Score.text = player.score.toString()
        binding.player2Button.isEnabled = model.currentIdPlayer == player.idPlayer
        binding.player2CbIA.isEnabled = model.currentIdPlayer == player.idPlayer

        player = model.players[3]
        binding.player3Icon.setBackgroundResource(player.idDisc)
        binding.player3Score.text = player.score.toString()
        binding.player3Button.isEnabled = model.currentIdPlayer == player.idPlayer
        binding.player3CbIA.isEnabled = model.currentIdPlayer == player.idPlayer
    }


    override fun onSaveInstanceState(outState: Bundle) {

        Log.d(TAG, "onSaveInstanceState($outState)");

        super.onSaveInstanceState(outState)

        //outState.putBoolean(KEY_MODEL, model.IsButtonClicked)
        outState.putParcelable(KEY_MODEL, model)


    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {

        Log.d(TAG, "onRestoreInstanceState($savedInstanceState)");

        super.onRestoreInstanceState(savedInstanceState)

        //model.IsButtonClicked = savedInstanceState.getBoolean(KEY_MODEL, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState.getParcelable(KEY_MODEL, Model::class.java)?.let {
                model = it
            }
        } else {
            savedInstanceState.getParcelable<Model>(KEY_MODEL)?.let {
                model = it
            }
        }


        refresh()

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

private operator fun <T> Array<T>.get(i: Int?): T? {
    i ?: return null
    return this[i]
}
