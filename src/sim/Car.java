package sim;

import java.util.ArrayList;
import java.util.TreeMap;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import sim.CarDefinition.WheelDefinition;

/**
 *
 * @author Jonah Shapiro
 *
 */
class Car {

    private static final int MAX_CAR_HEALTH = MainWindow.FPS;

    private Body chassis; //the chassis of the car
    //the following two arraylists are used to destroy the car
    private ArrayList<Body> wheels;
    private ArrayList<Joint> joints;
    private World world;

    private int health = MAX_CAR_HEALTH;
    //The following variables are used to check for car death and fitness score
    private float maxPositionx = 0F;
    private float maxPositiony = 0F;
    private float minPositiony = 0F;

    private CarDefinition definition;

    private float[] genome;

    Car(CarDefinition def, World world) {
        //initialize various objects
        this.world = world;
        this.genome = new float[22];
        this.definition = def;
        this.wheels = new ArrayList<>();
        this.joints = new ArrayList<>();
        writeGenome(); //write the given definition to a genome array
        this.chassis = createChassis(this.definition.getVertices()); // create chassis
        float carMass = this.chassis.getMass();
        // create wheels
        for (int i = 0; i < this.definition.getWheels().size(); i++) {
            if (this.definition.getWheels().get(i).getVertex() != -1) { //check if the wheel should exist
                Body wheel = createWheel(this.definition.getWheels().get(i)); //create the wheel
                this.wheels.add(wheel);
                carMass += wheel.getMass();
                this.joints.add(createJointForWheel(wheel, this.definition.getWheels().get(i), ((carMass) * (-MainWindow.GRAVITY.y / this.definition.getWheels().get(i).getRadius())))); //create the joint for the wheel
            }
        }
    }

    Car(float[] genome, World world) {
        this.world = world;
        this.genome = genome;
        this.definition = createDefinition();
        this.wheels = new ArrayList<>();
        this.joints = new ArrayList<>();
        this.chassis = createChassis(this.definition.getVertices()); // create chassis
        float carMass = this.chassis.getMass();
        // create wheels
        for (int i = 0; i < this.definition.getWheels().size(); i++) {
            if (this.definition.getWheels().get(i).getVertex() != -1) {
                Body wheel = createWheel(this.definition.getWheels().get(i));
                this.wheels.add(wheel);
                carMass += wheel.getMass();
                this.joints.add(createJointForWheel(wheel, this.definition.getWheels().get(i), (carMass * (-MainWindow.GRAVITY.y / this.definition.getWheels().get(i).getRadius()))));
            }
        }
    }

    /**
     * createDefinition
     *
     * @author Jonah Shapiro
     * @return the created definition
     */
    private CarDefinition createDefinition() {
        CarDefinition def = new CarDefinition();
        //create the definition vertices
        for (int i = 0; i < CarDefinition.NUM_VERTICES; i++) {
            def.addVertex(Util.polarToRectangular(this.genome[i * 2], this.genome[(i * 2) + 1]));
        }
        //create the definiton wheels
        for (int w = 0; w < CarDefinition.NUM_WHEELS; w++) {
            def.addWheel(def.new WheelDefinition(this.genome[(w * 2) + (CarDefinition.NUM_VERTICES * 2)], Util.nextFloat(25, 75), (int) this.genome[(w * 2) + 1 + (CarDefinition.NUM_VERTICES * 2)]));
        }
        return def;
    }

    /**
     * writeGenome
     *
     * @author Jonah Shapiro
     * @description This method accesses all of the definitions and uses their values to
     */
    private void writeGenome() {
        // write chassis
        ArrayList<Vec2> vertices = this.definition.getVertices();
        // vertices.remove(0);
        for (int i = 0; i < vertices.size(); i++) {
            float[] polar = Util.rectangularToPolar(vertices.get(i));
            this.genome[i * 2] = polar[0];
            this.genome[(i * 2) + 1] = polar[1];
        }
        // write wheels
        ArrayList<WheelDefinition> wheels = this.definition.getWheels();
        for (int i = 0; i < wheels.size(); i++) {
            this.genome[(i * 2) + (vertices.size() * 2)] = wheels.get(i).getRadius();
            this.genome[(i * 2) + 1 + (vertices.size() * 2)] = wheels.get(i).getVertex();
        }

    }

    /**
     * checkDeath
     * @description this method checks if the car has died as well and decrements its health each tick
     * @author Jonah Shapiro
     * @return A boolean representing whether or not the car is alive
     */
    boolean checkDeath() {
        Vec2 position = this.getPosition();

        if (position.y > this.maxPositiony) { //increment the maximum height reached
            this.maxPositiony = position.y;
        }

        if (position.y < minPositiony) { //decrement the minimum height reached
            this.minPositiony = position.y;
        }
        //check if the car is out of bounds
        if (position.x < 0.0F) {
            return true;
        }
        if (position.x > Ground.maxSegments) {
            this.maxPositionx = Ground.maxSegments;
            return true;
        }
        //if the car is moving fast enough, reset its health
        if (Math.abs(this.chassis.getLinearVelocity().y) > 0.01f) {
            this.health = MAX_CAR_HEALTH;
        }
        if (position.x > maxPositionx + 0.01f) {
            this.health = MAX_CAR_HEALTH;
            this.maxPositionx = position.x;
        } else {
            if (Math.abs(this.chassis.getLinearVelocity().x) < 0.01f) { //if the car is moving too slowly decrement its health
                this.health--;
            }
            if (position.x > maxPositionx) { //set the maximum distance travelled
                this.maxPositionx = position.x;
            }
            this.health--; //decrement the car's health
            return this.health <= 0;
        }
        return false;

    }

    /**
     * kill
     * @author Jonah Shapiro
     * @description This method destroys the car body
     */
    void kill() {
        for (Joint j : this.joints) { //destroy the joints
            this.world.destroyJoint(j);
        }
        for (Body wheel : this.wheels) { //destroy the wheels
            this.world.destroyBody(wheel);
        }
        this.world.destroyBody(this.chassis); //destroy the chassis
    }

    private Body createWheel(CarDefinition.WheelDefinition wheelDef) {
        //the following code initializes and sets various properties of the wheel body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyType.DYNAMIC;
        bodyDef.position = new Vec2(1.0F, 2.0F);
        Body body = world.createBody(bodyDef);
        FixtureDef fixtureDef = new FixtureDef(); //create the wheel fixture
        fixtureDef.shape = new CircleShape();
        fixtureDef.shape.setRadius(wheelDef.getRadius());
        fixtureDef.density = wheelDef.getDensity();
        fixtureDef.friction = 1F;
        fixtureDef.restitution = 0.2F;
        fixtureDef.filter.groupIndex = -1;

        body.createFixture(fixtureDef); //attach the fixture

        return body;

    }

    private Joint createJointForWheel(Body wheel, CarDefinition.WheelDefinition wheelDef, float torqueWheel) {
        RevoluteJointDef jointDefinition = new RevoluteJointDef();
        Vec2 randVec2 = this.definition.getVertices().get(wheelDef.getVertex()); //get the vertex that the wheel will be attached to
        //set the joint anchor bodies
        jointDefinition.bodyA = this.chassis;
        jointDefinition.bodyB = wheel;
        //set the joint anchor points
        jointDefinition.localAnchorA = randVec2;
        jointDefinition.localAnchorB = new Vec2(0F, 0F);
        jointDefinition.maxMotorTorque = torqueWheel;
        jointDefinition.motorSpeed = -CarDefinition.MOTOR_SPEED;
        jointDefinition.enableMotor = true;
        return world.createJoint(jointDefinition); //create the joint
    }

    private Body createChassis(ArrayList<Vec2> vertices) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyType.DYNAMIC;
        bodyDef.position = new Vec2(1.0F, 2.0F);
        Body body = world.createBody(bodyDef);
        //The following block of code sorts the list of polar coordinates in descending order based on their angle
        //This is done in order to connect the points using non-intersecting triangles
        TreeMap<Float, Float> points = new TreeMap<>();
        for (Vec2 vertice : vertices) {
            float[] polar = Util.rectangularToPolar(vertice);
            points.put(polar[1], polar[0]); // key is angle, value is magnitude
        }
        ArrayList<Vec2> sorted = new ArrayList<>();
        ArrayList<Float> keys = new ArrayList<>(points.keySet());
        for (int i = 0; i < points.size(); i++) {
            sorted.add(Util.polarToRectangular(points.get(keys.get(i)), keys.get(i)));
        }
        // create chassis parts
        for (int part = 1; part < sorted.size(); part++) {
            createChassisPart(body, sorted.get(part - 1), sorted.get(part));
        }
        return body;
    }

    /**
     * createChassisPart
     *
     * @author Jonah Shapiro
     * @param body The chassis body
     * @param one the first point
     * @param two the second point
     */
    private void createChassisPart(Body body, Vec2 one, Vec2 two) {
        Vec2[] listOfVertices = new Vec2[3];
        listOfVertices[0] = one;
        listOfVertices[1] = two;
        listOfVertices[2] = new Vec2(0, 0);
        PolygonShape s = new PolygonShape();
        s.set(listOfVertices, 3); //set the vertices of the polygon
        //create and initialize the polygon fixture
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = s;
        fixtureDef.density = CarDefinition.CHASSIS_DENSITY;
        fixtureDef.friction = 10F;
        fixtureDef.restitution = 0.2F;
        fixtureDef.filter.groupIndex = -1;
        body.createFixture(fixtureDef);
    }

    private Vec2 getPosition() {
        return chassis.getPosition();
    }

    float[] getGenome() {
        return this.genome;
    }

    float getFitnessScore() {
        return this.maxPositionx;
    }

}
