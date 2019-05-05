package Project.FrontEnd

import javafx.scene.input.{KeyCode, KeyEvent}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.paint.Color
import scalafx.scene.shape.{Rectangle, Shape}
import scalafx.scene.{Group, Scene}
import io.socket.client.{IO, Socket}
import io.socket.emitter.Emitter
import javafx.application.Platform
import play.api.libs.json.{JsValue, Json}

class HandleMessagesFromPython() extends Emitter.Listener {
  override def call(objects: Object*): Unit = {
    Platform.runLater(() => {
      val jsonGameState = objects.apply(0).toString
      val gameState: JsValue = Json.parse(jsonGameState)
      val mapped: Map[String, Map[String, Int]] = gameState.as[Map[String, Map[String, Int]]]
      DesktopVersion.generatePlayers(mapped)
    })
  }
}

object DesktopVersion extends JFXApp {

  var socket: Socket = IO.socket("http://localhost:8080/")
  socket.on("gameState", new HandleMessagesFromPython)

  socket.connect()
  socket.emit("connect")

  val gridWidth: Double = 30
  val gridHeight: Double = 30

  val windowWidth: Double = gridWidth * 38
  val windowHeight: Double = gridHeight * 19

  val playerSpriteSize: Double = 20
  val lineThickness = 0.1

  var platformSprites: Map[Int, Shape] = Map[Int, Shape]()

  var sceneGraphics: Group = new Group {}
  var sceneGrid: Group = new Group {}

  griditize()

  def keyPressed(keyCode: KeyCode): Unit = {
    keyCode.getName match {
      case "W" => socket.emit("keyStates", "w")
      case "A" => socket.emit("keyStates", "a")
      case "S" => socket.emit("keyStates", "s")
      case "D" => socket.emit("keyStates", "d")
    }
  }

  def playerSprite(xLocation: Double, yLocation: Double, color: Color): Shape = {
    new Rectangle {
      width = playerSpriteSize
      height = playerSpriteSize
      translateX = (xLocation - 1) * gridWidth + 5
      translateY = (yLocation - 1) * gridHeight + 5
      fill = color
    }
  }

  def generatePlayers(gameState: Map[String, Map[String, Int]]): Unit = {
    sceneGraphics.getChildren.clear()
    var sprite: Shape = null
    for((player, value) <- gameState){
      if(socket.id() == player){
        sprite = playerSprite(value("x"), value("y"), Color.web("#FF4500"))
      }else{
        sprite = playerSprite(value("x"), value("y"), Color.web("#483D8B"))
      }
      sceneGraphics.children.add(sprite)
    }
  }

  def verticalLine(xval: Int): Rectangle = {
    new Rectangle() {
      width = lineThickness
      height = windowHeight
      translateX = gridWidth * xval
      fill = Color.Black
    }
  }

  def horizontalLine(yval: Int): Rectangle = {
    new Rectangle() {
      width = windowWidth
      height = lineThickness
      translateY = gridHeight * yval
      fill = Color.Black
    }
  }

  def griditize(): Unit = {
    for(i <- 1 to 38){
      sceneGrid.children.add(verticalLine(i))
    }
    for(j <- 1 to 19){
      sceneGrid.children.add(horizontalLine(j))
    }
  }

  this.stage = new PrimaryStage {
    this.title = "Desktop App1"
    scene = new Scene(windowWidth, windowHeight) {
      content = List(sceneGrid, sceneGraphics)
      addEventHandler(KeyEvent.KEY_PRESSED, (event: KeyEvent) => keyPressed(event.getCode))
    }
  }

}
