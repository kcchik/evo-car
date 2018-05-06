package sim;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import java.util.ArrayList;

/**
 * MainWindow.java
 * @author Kevin Chik and Anthony Lai
 * 22/12/2016
 */
public class MainWindow extends Application {

    //world
    static final Vec2 GRAVITY = new Vec2(0.0F, -9.81F);
    private World world = new World(GRAVITY);
    static final int FPS = 60;
    private Timeline timeline = new Timeline();

    //stage
    private static final int WIDTH = 900;
    private static final int HEIGHT = 600;
    private float[] carPos = new float[2];
    private float[] camera = new float[2];

    //presets
    private double MUTATION_RATE = 0.2;
    private double MUTATION_EFFECT = 0.5;
    private int populationSize = 20;
    private int selectionType = 0; //0- Roulette //1 - Tournament

    //algorithm
    private int generation = 0;
    private int carNumber = 0;
    private float[][] currentGenome = new float[populationSize][22];
    private float[][] genome = new float[populationSize][22];
    private double[] distance = new double[populationSize];
    private static int carsGenerated = 0;

    //body list
    private Body[] bodyList;
    private Shape[][][] shapeList;

    //text and fields
    private Text carInfoText;
    private Text carFitnessScoreText;

    private Label populationSizeSliderLabel;
    private Label mutationRateSliderLabel;
    private Label mutationEffectSliderLabel;
    private Label numTilesPresetSliderLabel;

    private Slider mutationRateSlider;
    private Slider mutationEffectSlider;
    private Slider populationSizeSlider;
    private Slider numTilesPresetSlider;
    private Slider selectionTypeChoice;

    //map maker
    private boolean customMap = false;
    private ArrayList<float[]> mapCoordinates = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        menu(primaryStage);
    }

    private void menu(Stage primaryStage) {
        //stage settings
        primaryStage.setWidth(WIDTH);
        primaryStage.setHeight(HEIGHT);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setStyle("-fx-background: #FFFFFF;");

        Text sceneTitle = new Text("Evo Car");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 1, 3, 1);

        Button startButton = new Button("Start");
        Button presetsButton = new Button("Presets");
        Button mapButton = new Button("Map maker");
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.BOTTOM_CENTER);
        hBox.getChildren().add(startButton);
        hBox.getChildren().add(presetsButton);
        hBox.getChildren().add(mapButton);
        grid.add(hBox, 0, 2);

        startButton.setOnAction(event -> startSimulation(primaryStage));
        presetsButton.setOnAction(event -> presets(primaryStage));
        mapButton.setOnAction(event -> mapMaker(primaryStage));

        Scene scene = new Scene(grid, WIDTH, HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * startSimulation
     * start the program
     * @author Kevin Chik
     * @param primaryStage stage
     */
    private void startSimulation(Stage primaryStage) {
        //root
        Group root = new Group();

        //ground
        Ground ground = new Ground(world);
        if (customMap) {
            ground.customGround(mapCoordinates);
        } else {
            ground.createGround();
        }
        createBodyList();
        createShapeList();

        //text
        drawText(root);
        carFitnessScoreText = new Text();

        drawFitnessScoreText(root);


        Button backButton = new Button("Back");
        root.getChildren().add(backButton);

        backButton.setOnAction(event -> backSimulation(primaryStage));

        //create scene
        Scene scene = new Scene(root);

        //set scene
        primaryStage.setScene(scene);
        primaryStage.show();

        runGeneticAlgorithm(root);
    }

    /**
     * backSimulation
     * terminates simulation and returns to menu
     * @author Kevin Chik
     * @param primaryStage stage
     */
    private void backSimulation(Stage primaryStage) {
        timeline.stop();
        customMap = false;
        mapCoordinates = new ArrayList<>();
        genome = new float[20][22];
        generation = 0;
        carNumber = 0;
        carsGenerated = 0;
        world = new World(GRAVITY);
        menu(primaryStage);
    }

    /**
     * presets
     * preset ranges for algorithm
     * @author Kevin Chik and Anthony Lai
     * @param primaryStage stage
     */
    private void presets(Stage primaryStage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label selectionTypeCaption = new Label ("Selection Method\n(0 = Roulette, 1 = Tournament)");
        grid.add (selectionTypeCaption,0,0);

        selectionTypeChoice = new Slider(0,1,selectionType);
        selectionTypeChoice.setBlockIncrement(1);
        selectionTypeChoice.setMinorTickCount(0);
        selectionTypeChoice.setMajorTickUnit(1);
        selectionTypeChoice.setSnapToTicks(true);
        selectionTypeChoice.setShowTickMarks(true);
        selectionTypeChoice.setShowTickLabels(true);
        grid.add (selectionTypeChoice, 1, 0);


        Label populationSizeLabel = new Label("Population Size");
        grid.add(populationSizeLabel, 0, 1);

        populationSizeSlider = new Slider(12,60,populationSize);
        populationSizeSlider.setBlockIncrement(4);
        populationSizeSlider.setMinorTickCount(2);
        populationSizeSlider.setMajorTickUnit(12);
        populationSizeSlider.setSnapToTicks(true);
        populationSizeSlider.setShowTickMarks(true);
        populationSizeSlider.setShowTickLabels(true);
        grid.add(populationSizeSlider,1,1);

        populationSizeSliderLabel = new Label(Math.round(populationSizeSlider.getValue())+"");
        grid.add(populationSizeSliderLabel,2,1);

        populationSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> populationSizeSliderLabel.setText((Math.round(populationSizeSlider.getValue()/4)*4)+""));

        Label mutationRateLabel = new Label("Mutation rate:");
        grid.add(mutationRateLabel, 0, 2);

        mutationRateSlider = new Slider(0.0, 1.0, MUTATION_RATE);
        mutationRateSlider.setBlockIncrement(0.1);
        mutationRateSlider.setMajorTickUnit(0.1);
        mutationRateSlider.setMinorTickCount(0);
        mutationRateSlider.setSnapToTicks(true);
        mutationRateSlider.setShowTickMarks(true);
        mutationRateSlider.setShowTickLabels(true);
        grid.add(mutationRateSlider,1,2);

        mutationRateSliderLabel = new Label(mutationRateSlider.getValue()+"");
        grid.add(mutationRateSliderLabel,2,2);

        mutationRateSlider.valueProperty().addListener((observable, oldValue, newValue) -> mutationRateSliderLabel.setText((((double)(Math.round((mutationRateSlider.getValue()*100)*10)/100))/10)+""));

        Label mutationEffectLabel = new Label("Mutation Effect:");
        grid.add(mutationEffectLabel, 0, 3);

        mutationEffectSlider = new Slider(0.0,1.0,MUTATION_EFFECT);
        mutationEffectSlider.setBlockIncrement(0.1);
        mutationEffectSlider.setMinorTickCount(0);
        mutationEffectSlider.setMajorTickUnit(0.1);
        mutationEffectSlider.setSnapToTicks(true);
        mutationEffectSlider.setShowTickMarks(true);
        mutationEffectSlider.setShowTickLabels(true);
        grid.add(mutationEffectSlider, 1,3);

        mutationEffectSliderLabel = new Label(mutationEffectSlider.getValue()+"");
        grid.add(mutationEffectSliderLabel,2,3);

        mutationEffectSlider.valueProperty().addListener((observable, oldValue, newValue) -> mutationEffectSliderLabel.setText((((double)(Math.round((mutationEffectSlider.getValue()*100)*10)/100))/10)+""));


        Label numTilesPresetLabel = new Label("Number of Tiles in Map");
        grid.add(numTilesPresetLabel, 0, 4);

        numTilesPresetSlider = new Slider(100,700,Ground.maxSegments);
        numTilesPresetSlider.setBlockIncrement(100);
        numTilesPresetSlider.setMinorTickCount(1);
        numTilesPresetSlider.setMajorTickUnit(100);
        numTilesPresetSlider.setSnapToTicks(true);
        numTilesPresetSlider.setShowTickMarks(true);
        numTilesPresetSlider.setShowTickLabels(true);
        grid.add(numTilesPresetSlider, 1,4);

        numTilesPresetSliderLabel = new Label(numTilesPresetSlider.getValue()+"");
        grid.add(numTilesPresetSliderLabel,2,4);

        numTilesPresetSlider.valueProperty().addListener((observable, oldValue, newValue) -> numTilesPresetSliderLabel.setText((Math.round(numTilesPresetSlider.getValue()/50)*50)+""));

        Button backButton = new Button("Back");
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.BOTTOM_LEFT);
        hBox.getChildren().add(backButton);
        grid.add(hBox, 0, 5);

        backButton.setOnAction(event -> backPresets(primaryStage));

        Scene scene = new Scene(grid, WIDTH, HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * backPresets
     * updates presets and returns to menu
     * @author Kevin Chik and Anthony Lai
     * @param primaryStage stage
     */
    private void backPresets(Stage primaryStage) {
        populationSize = (int) populationSizeSlider.getValue();
        MUTATION_RATE = (((double)(Math.round((mutationRateSlider.getValue()*100)*10)/100))/10);
        MUTATION_EFFECT = (((double)(Math.round((mutationEffectSlider.getValue()*100)*10)/100))/10);
        Ground.maxSegments = (int) numTilesPresetSlider.getValue();
//        selectionType = selectionTypeChoice.getSelectionModel().getSelectedIndex();
        selectionType = (int) selectionTypeChoice.getValue();


        menu(primaryStage);
    }

    /**
     * mapMaker
     * create custom map
     * @author Kevin Chik
     * @param primaryStage stage
     */
    private void mapMaker(Stage primaryStage) {
        //root
        Group root = new Group();

        Rectangle canvas = new Rectangle();
        canvas.setHeight(HEIGHT);
        canvas.setWidth(WIDTH);
        canvas.setFill(Color.WHITE);
        root.getChildren().add(canvas);

        ArrayList<Rectangle> shapes = new ArrayList<>();
        ArrayList<float[]> angles = new ArrayList<>();
        double[] position = new double[2];

        canvas.setOnMouseClicked(event -> {
            if (event.getSceneX() >= 300) {
                float x = (float) event.getSceneX() - 300;
                float y = (float) event.getSceneY() - 300;
                float[] angle = {(float) Math.atan(-y / x)};
                mapCoordinates.add(angle);
                for (int i = 0; i < shapes.size(); i++) {
                    shapes.get(i).getTransforms().remove(0);
                    shapes.get(i).setX(shapes.get(i).getX() - position[0]);
                    shapes.get(i).setY(shapes.get(i).getY() - position[1]);
                    Rotate revert = new Rotate((double) -angles.get(i)[0] / Math.PI * 180, shapes.get(i).getX(), shapes.get(i).getY());
                    shapes.get(i).getTransforms().add(revert);
                }
                position[0] = 50 * Math.cos(Math.atan(y / x));
                position[1] = 50 * Math.sin(Math.atan(y / x));
                Rectangle rectangle = new Rectangle();
                rectangle.setHeight(10);
                rectangle.setWidth(50);
                rectangle.setX(300);
                rectangle.setY(300);
                rectangle.setFill(Color.TRANSPARENT);
                rectangle.setStroke(Color.GRAY);
                Rotate rotation = new Rotate(Math.atan(y / x) / Math.PI * 180, rectangle.getX(), rectangle.getY());
                rectangle.getTransforms().add(rotation);
                shapes.add(rectangle);
                angles.add(angle);
                root.getChildren().add(rectangle);
            }
        });

        Button backButton = new Button("Back");
        root.getChildren().add(backButton);

        backButton.setOnAction(event -> backMapMaker(primaryStage));

        //create scene
        Scene scene = new Scene(root);

        //set scene
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * backMapMaker
     * implements custom map and returns to menu
     * @param primaryStage stage
     */
    private void backMapMaker(Stage primaryStage) {
        customMap = true;
        menu(primaryStage);
    }

    /**
     * runGeneticAlgorithm
     * genetic algorithm
     * @author Kevin Chik and Anthony Lai
     * @param root group that contains all shapes to be displayed
     */
    private void runGeneticAlgorithm(Group root) {
        Car car;
        if (generation > 0) {
            car = new Car(genome[carNumber], world);
        } else {
            car = new Car(CarDefinition.createRandomCar(), world);
        }
        createBodyList();
        createShapeList();
        drawCar(root);
        drawGround(root);
        carInfoText.setText("Generation: " + generation + "\nCar number: " + (carNumber + 1) + "\nTotal cars generated: " + (carsGenerated + 1));

        //evaluate
        timeline = new Timeline();
        evaluate(car, root);
    }

    /**
     * evaluate
     * sets up keyframes for every 1/60s
     * @author Kevin Chik and Anthony Lai
     */
    private void evaluate(Car car, Group root) {
        timeline.setCycleCount(Timeline.INDEFINITE);
        Duration duration = Duration.seconds(1.0 / FPS);
        EventHandler<ActionEvent> actionEvent = terminate -> {
            world.step(1.0f / FPS, 8, 3);
            createBodyList();
            carFitnessScoreText.setText("Fitness Score: " + Util.round2(car.getFitnessScore()) + "");
            update();
            if (car.checkDeath()) {
                currentGenome[carNumber] = car.getGenome();
                distance[carNumber] = car.getFitnessScore();
                clearScreen(root);
                car.kill();
                carsGenerated++;
                carNumber++;
                timeline.pause();
                if (carNumber == populationSize) {
                    if (selectionType == 0) {
                        genome = rouletteSelection(currentGenome, distance);
                    }else if (selectionType == 1){
                        genome = tournamentSelection(currentGenome, distance);
                    }
                    carNumber = 0;
                    generation++;
                }
                runGeneticAlgorithm(root);
            }
        };
        KeyFrame keyFrame = new KeyFrame(duration, actionEvent, null, null);
        timeline.getKeyFrames().add(keyFrame);
        timeline.playFromStart();
    }

    /**
     * clearScreen
     * removes shapes from root
     * @author Kevin Chik
     * @param root group that contains all shapes to be displayed
     */
    private void clearScreen(Group root) {
        for (Shape[][] body : shapeList) {
            for (Shape[] fixture : body) {
                for (Shape line : fixture) {
                    root.getChildren().remove(line);
                }
            }
        }
    }

    /**
     * update
     * updates position of car
     * @author Kevin Chik
     */
    private void update() {
        for (int i = 0; i < shapeList.length; i++) {
            float x = Util.toPixelX(bodyList[i].getPosition().x) + 200f;
            float y = Util.toPixelY(bodyList[i].getPosition().y) - 800;
            float angle = -(bodyList[i].getTransform().q.getAngle());
            Fixture fixture = bodyList[i].getFixtureList();
            int fixtureCount = 0;
            do {
                fixtureCount++;
                fixture = fixture.getNext();
            } while (fixture != null);
            if (fixtureCount > 1) {
                camera[0] = carPos[0] - x;
                camera[1] = carPos[1] - y;
            }
            x += camera[0];
            y += camera[1];
            fixture = bodyList[i].getFixtureList();
            for (int j = 0; j < shapeList[i].length; j++) {
                if (fixture.getType() == ShapeType.POLYGON) {
                    PolygonShape shape = (PolygonShape)fixture.getShape();
                    int k = 0;
                    for (; k < shapeList[i][j].length - 1; k++) {
                        Line line = ((Line)shapeList[i][j][k]);
                        float x0 = shape.getVertex(k).x;
                        float y0 = shape.getVertex(k).y;
                        float ax0 = (float) (Math.sin(angle) * y0 + Math.cos(angle) * x0);
                        float ay0 = (float) (Math.cos(angle) * y0 - Math.sin(angle) * x0);
                        float x1 = shape.getVertex(k + 1).x;
                        float y1 = shape.getVertex(k + 1).y;
                        float ax1 = (float) (Math.sin(angle) * y1 + Math.cos(angle) * x1);
                        float ay1 = (float) (Math.cos(angle) * y1 - Math.sin(angle) * x1);
                        if (shapeList[i][j].length > 3) {
                            line.setStroke(Color.GRAY);
                        } else {
                            line.setStroke(Color.valueOf("#2b2b2b"));
                        }
                        line.setStartX(Util.toPixelX(ax0) + x);
                        line.setStartY(Util.toPixelY(ay0) + y);
                        line.setEndX(Util.toPixelX(ax1) + x);
                        line.setEndY(Util.toPixelY(ay1) + y);
                        shapeList[i][j][k] = line;
                    }
                    Line line = ((Line)shapeList[i][j][k]);
                    float x0 = shape.getVertex(0).x;
                    float y0 = shape.getVertex(0).y;
                    float ax0 = (float) (Math.sin(angle) * y0 + Math.cos(angle) * x0);
                    float ay0 = (float) (Math.cos(angle) * y0 - Math.sin(angle) * x0);
                    float x1 = shape.getVertex(k).x;
                    float y1 = shape.getVertex(k).y;
                    float ax1 = (float) (Math.sin(angle) * y1 + Math.cos(angle) * x1);
                    float ay1 = (float) (Math.cos(angle) * y1 - Math.sin(angle) * x1);
                    if (shapeList[i][j].length > 3) {
                        line.setStroke(Color.GRAY);
                    } else {
                        line.setStroke(Color.valueOf("#2b2b2b"));
                    }
                    line.setStartX(Util.toPixelX(ax0) + x);
                    line.setStartY(Util.toPixelY(ay0) + y);
                    line.setEndX(Util.toPixelX(ax1) + x);
                    line.setEndY(Util.toPixelY(ay1) + y);
                    shapeList[i][j][k] = line;
                } else if (fixture.getType() == ShapeType.CIRCLE) {
                    CircleShape shape = (CircleShape)fixture.getShape();
                    Circle circle = ((Circle)shapeList[i][j][0]);
                    circle.setFill(Color.rgb(255, 192, 203, 0.5));
                    circle.setStroke(Color.DEEPPINK);
                    circle.setRadius(shape.getRadius() * 50f);
                    circle.setCenterX(x);
                    circle.setCenterY(y + 600f);
                    shapeList[i][j][0] = circle;
                }
                fixture = fixture.getNext();
            }
        }

    }

    /**
     * draw
     * draws ground on scene
     * @author Kevin Chik
     * @param root group that contains all shapes to be displayed
     */
    private void drawGround(Group root) {
        for (Shape[][] body : shapeList) {
            for (Shape[] fixture : body) {
                if (fixture.length == 4) {
                    for (Shape line : fixture) {
                        root.getChildren().add(line);
                    }
                }
            }
        }
    }

    /**
     * drawCar
     * draws car on scene
     * @author Kevin Chik
     * @param root group that contains all shapes to be displayed
     */
    private void drawCar(Group root) {
        for (Body body : bodyList) {
            int fixtureCount = 0;
            Fixture fixture = body.getFixtureList();
            do {
                fixtureCount++;
                fixture = fixture.getNext();
            } while (fixture != null);
            if (fixtureCount > 1) {
                carPos[0] = Util.toPixelX(body.getPosition().x) + 200f;
                carPos[1] = Util.toPixelY(body.getPosition().y) - 800f;
            }
        }
        for (Shape[][] body : shapeList) {
            for (Shape[] fixture : body) {
                if (fixture.length < 4) {
                    for (Shape line : fixture) {
                        root.getChildren().add(line);
                    }
                }
            }
        }
    }

    /**
     * createBodyList
     * puts all JBox2d bodies in an array
     * @author Kevin Chik
     */
    private void createBodyList() {
        bodyList = new Body[world.getBodyCount()];
        Body body = world.getBodyList();
        for (int i = 0; i < world.getBodyCount(); i++) {
            bodyList[i] = body;
            body = body.getNext();
        }
    }

    /**
     * createShapeList
     * converts bodyList to JavaFX shapes
     * @author Kevin Chik
     */
    private void createShapeList() {
        shapeList = new Shape[bodyList.length][][];
        for (int i = 0; i < world.getBodyCount(); i++) {
            int fixtureCount = 0;
            Fixture fixture = bodyList[i].getFixtureList();
            do {
                fixtureCount++;
                fixture = fixture.getNext();
            } while (fixture != null);
            shapeList[i] = new Shape[fixtureCount][];
            fixture = bodyList[i].getFixtureList();
            for (int j = 0; j < fixtureCount; j++) {
                if (fixture.getType() == ShapeType.POLYGON) {
                    int vertexCount = ((PolygonShape)fixture.getShape()).getVertexCount();
                    shapeList[i][j] = new Line[vertexCount];
                    int k = 0;
                    for (; k < vertexCount - 1; k++) {
                        shapeList[i][j][k] = new Line();
                    }
                    shapeList[i][j][k] = new Line();
                } else if (fixture.getType() == ShapeType.CIRCLE) {
                    shapeList[i][j] = new Circle[1];
                    shapeList[i][j][0] = new Circle();
                }
                fixture = fixture.getNext();
            }
        }
    }

    /**
     * drawText
     * shows info on algorithm
     * @author Kevin Chik
     * @param root group that contains all shapes to be displayed
     */
    private void drawText(Group root) {
        carInfoText = new Text();
        carInfoText.setFont(new Font(12));
        carInfoText.setX(5);
        carInfoText.setY(520);
        root.getChildren().add(carInfoText);
    }

    /**
     * drawText
     * shows info on algorithm
     * @author Anthony Lai
     * @param root group that contains all shapes to be displayed
     */
    private void drawFitnessScoreText(Group root) {
        carFitnessScoreText.setFont(new Font(12));
        carFitnessScoreText.setX(5);
        carFitnessScoreText.setY(506);
        root.getChildren().add(carFitnessScoreText);
    }

    /**
     * rouletteSelection
     * determines parents for next generation
     * @author Anthony Lai
     * @param currentGen current generation of cars
     * @param distance fitness scores
     * @return parents for next generation
     */
    private float[][] rouletteSelection(float[][] currentGen, double[] distance){

        //fitnessScores - index 0 is the car's fitness score - index 1 is the car's probability of selection
        double [][] fitnessScores = new double[populationSize][2];
        for (int i = 0; i < fitnessScores.length; i++) {
            fitnessScores[i][0] = distance[i];
        }

        //Find sum of all fitness scores
        double sumOfFitnessScores = 0;
        for (double[] fitnessScore : fitnessScores) {
            sumOfFitnessScores = fitnessScore[0] + sumOfFitnessScores;
        }

        //Find each car's probability of selection
        for (int i = 0; i < fitnessScores.length; i++){
            fitnessScores[i][1] = (fitnessScores[i][0] / sumOfFitnessScores) * 100;
        }

        double[] rouletteWheel = new double[populationSize];
        rouletteWheel[0] = fitnessScores[0][1];
        for (int i = 1; i < rouletteWheel.length; i++){
            rouletteWheel[i] = fitnessScores[i][1] + rouletteWheel[i-1];
        }

        //selecting parents
        double selectionNum;
        ArrayList<float[]> parents = new ArrayList<>();
        boolean[] selected = new boolean[populationSize];
        do{
            selectionNum = (Math.random()*101);

            if ((selectionNum >= 0) && (selectionNum <= rouletteWheel[0])){
                if (!selected[0]) {
                    parents.add(currentGen[0]);
                    selected[0] = true;
                }
            }
            for (int j = 1; j < rouletteWheel.length; j++){
                if ((selectionNum > rouletteWheel[j-1]) && (selectionNum <= rouletteWheel[j])){
                    if (!selected[j]) {
                        parents.add(currentGen[j]);
                        selected[j] = true;
                    }
                }
            }
        }while (parents.size() < populationSize/2);

        //Call Crossover method
        //next step
        return crossover(parents);
    }

    /**
     * tournamentSelection
     * determines parents for next generation, tournament style
     * @author Anthony Lai
     * @param currentGen current generation of cars
     * @param distance fitness scores
     * @return parents for next generation
     */
    private float[][] tournamentSelection (float[][]currentGen, double[] distance){
        ArrayList<float[]>parents = new ArrayList<>();
        boolean[] selected = new boolean[populationSize];

        do{
            int carA = (int)(Math.random()*populationSize);
            int carB = (int)(Math.random()*populationSize);

            if (carA != carB){
                if ((!selected[carA])&&(!selected[carB])){
                    if (distance[carA] > distance[carB]){
                        parents.add(currentGen[carA]);
                        selected[carA] = true;
                        selected[carB] = true;
                    }else if (distance[carA] < distance[carB]){
                        parents.add(currentGen[carB]);
                        selected[carA] = true;
                        selected[carB] = true;
                    }else{
                        selected[carA] = false;
                        selected[carB] = false;
                    }
                }
            }
        }while(parents.size() < populationSize/2);

        return crossover(parents);
    }

    /**
     * crossover
     * performs crossover to create child generation
     * @author Kevin Chik (Validation Part by Anthony Lai)
     * @param parents parent generation
     * @return child generation
     */
    private float [][] crossover (ArrayList<float[]> parents){
        float[][] children = new float[populationSize][22];
        int i = 0;
        for (int two = 0; two < 2; two++) {
            for (int j = 0; j < parents.size(); j++) {
                float[] temp = parents.get(j);
                int random = (int) (Math.random() * parents.size());
                parents.set(j, parents.get(random));
                parents.set(random, temp);
            }

            for (int j = 0; j < parents.size(); j += 2) {
                float[] parent0 = parents.get(j);
                float[] parent1 = parents.get(j + 1);

                boolean valid = false;
                float[] genome0 = new float[22];
                float[] genome1 = new float[22];

                do {
                    int point0 = ((int) (Math.random() * 11) + 1) * 2 - 1;
                    int point1;
                    do {
                        point1 = ((int) (Math.random() * 11) + 1) * 2 - 1;
                    } while (point0 == point1);

                    if (point0 > point1) {
                        int temp = point0;
                        point0 = point1;
                        point1 = temp;
                    }

                    for (int k = 0; k < point0 - 1; k++) {
                        genome0[k] = parent0[k];
                        genome1[k] = parent1[k];
                    }
                    for (int k = point0 - 1; k < point1 - 1; k++) {
                        genome0[k] = parent1[k];
                        genome1[k] = parent0[k];
                    }
                    for (int k = point1 - 1; k < genome0.length; k++) {
                        genome0[k] = parent0[k];
                        genome1[k] = parent1[k];
                    }

                    ArrayList<Vec2> child1Vertices = new ArrayList<>();
                    ArrayList<Vec2> child2Vertices = new ArrayList<>();
                    for (int k = 0; k < genome0.length - 7; k+=2){
                        child1Vertices.add(Util.polarToRectangular(genome0[k], genome0[k+1]));
                        child2Vertices.add(Util.polarToRectangular(genome1[k], genome1[k+1]));
                    }

                    for (int k = 0; k < child1Vertices.size(); k++){
                        valid = CarDefinition.checkValid(child1Vertices.get(k), child1Vertices);
                        if (!valid){
                            break;
                        }
                    }

                    if (valid) {
                        for (int k = 0; k < child2Vertices.size(); k++) {
                            valid = CarDefinition.checkValid(child2Vertices.get(k), child2Vertices);
                            if (!valid) {
                                break;
                            }
                        }
                    }

                    child1Vertices.clear();
                    child2Vertices.clear();

                }while(!valid);

                children[i] = genome0;
                children[i + 1] = genome1;
                i += 2;
            }
        }

        return mutation(children);
    }

    /**
     * mutation
     * mutates children
     * @author Kevin Chik
     * @param children child generation
     * @return mutated child generation
     */
    private float[][] mutation(float[][] children){
        for (int i = 0; i < children.length; i++) {
            for (int j = 0; j < children[i].length; j++) {
                double random = Math.random();
                if (random <= MUTATION_RATE) {
                    float mutation = (float) (Math.random() * MUTATION_EFFECT * 2 - MUTATION_EFFECT);
                    children[i][j] += mutation;
                }
            }
        }
        return children;
    }

    public static void main(String[] args) {
        launch(args);
    }

}
