import basicneuralnetwork.NeuralNetwork;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;

public class Enemy extends Character{
    int index;

    NeuralNetwork brain;
    double score; // score for genetic algorithm
    float distTraveled = 0; // total dist traveled from spawn to death
    ArrayList<PVector> rounte = new ArrayList<>(); // route counted outside the road
    ArrayList<PVector> rounteOnRoad = new ArrayList<>(); // route counted on the road
    ArrayList<PVector> totalRoute = new ArrayList<>(); // total route
    int creationTime; // time measured in frames from beginning of the app to creation of enemy
    int timeLimit = 600; // limits how many frames enemy can live
    int timeLived; // how many frames did enemy lived
    boolean timeLimitActive = true;
    boolean isBest = false;
    int frameCountFactor = 1;
    PVector targetPos;
    float targetAngle;
    float closestDist = p.width;
    float targetDist;

    PVector closestBulletPos;
    float closestBulletAngle;
    float closestBulletDist= 0;

    float turningAcceleration;
    float turningSpeed;
    float maxTurningSpeed = 15;

    Ray[] rays = new Ray[4];
    boolean isOnRoad = true;

    //double[] readingsProjectile = new double[8];
    float angleStep = p.radians(10);
    float[][] sensors = new float[8][2];
    float[] rayReadings = new float[rays.length];
    double[] readingsPlayer = new double[sensors.length];

    int n = 0;
    PImage sprite;

    public Enemy(PApplet p, PVector position, Look look, float speed, float health) {
        super(p, position, look, speed, health);
        sprite = p.loadImage("Sprite.png");
        brain = new NeuralNetwork(sensors.length+rayReadings.length+2,2,8,1);
        creationTime = p.frameCount;
        createRays();
        createSensors();

    }

    public Enemy(PApplet p, PVector position, Look look, float speed, float health, NeuralNetwork brain) {
        super(p, position, look, speed, health);
        this.brain = brain;
        sprite = p.loadImage("Sprite.png");
        creationTime = p.frameCount;
        createRays();
        createSensors();
    }

    public void update(){

        // check if enemy is the best and change it's color accordingly
        if(isBest == true){
            look.changeColor(new int[]{230,67,34});
        } else if (isBest == false){
            look.changeColor(new int[]{163,160,59});
        }

        if(health<=0){
            this.isDead = true;
        }

        // if timeLived is bigger than timeLimit then mark as dead
        if(timeLived>timeLimit && timeLimitActive){
            this.isDead = true;
        }

        //calculate dist to the target
        targetDist = position.dist(targetPos);
        if(targetDist<10){
            this.isDead = true;
        }

        // calculate distTraveled
        if(timeLived%5==0){
            if(isOnRoad){
                rounteOnRoad.add(position.copy());
                totalRoute.add(position.copy());
            } else {
                rounte.add(position.copy());
                totalRoute.add(position.copy());
            }
        }


        // calculate score when dist is lower than ever
        if(targetDist<closestDist){
            closestDist = targetDist;
            score = (1/targetDist)*1000;
        }


        // calculate rotations and targetAngle to be from range of -PI to PI
        n  = (int) (rotation/(p.PI*2));
        rotation-=n*p.PI*2;
        targetAngle = calculateAngleToTarget(targetPos) - rotation;

        if(targetAngle<-p.PI){
            targetAngle = (2*p.PI) + targetAngle;
        } else if(targetAngle>p.PI){
            targetAngle = targetAngle - (2*p.PI);
        }

        sensors();

        brainActivity();

        //rotation = 0;
        //speed = 0;
    }

    public void brainActivity(){
        double inputs[] = new double[brain.getInputNodes()];

        for(int i = 0;i<readingsPlayer.length;i++){
            inputs[i] = readingsPlayer[i];
        }

        for(int i = readingsPlayer.length;i<readingsPlayer.length+rayReadings.length;i++){
            inputs[i] = rayReadings[i-readingsPlayer.length];
        }

        inputs[inputs.length-2] = targetDist/p.width;
        inputs[inputs.length-1] = isOnRoad ? 1 : 0;
//        for(int i = 18;i<18+8;i++){
//            inputs[i] = readingsProjectile[i-18];
//        }
        double output;
        output = brain.guess(inputs)[0];

        turningAcceleration = p.map((float) output,0,1,-8f,8f);

        if(turningSpeed<maxTurningSpeed && turningSpeed>-maxTurningSpeed){
            turningSpeed+=turningAcceleration;
        } else if(turningSpeed>maxTurningSpeed && turningAcceleration<0){
            turningSpeed+=turningAcceleration;
        } else if(turningSpeed<-maxTurningSpeed && turningAcceleration>0){
            turningSpeed+=turningAcceleration;
        }

        rotation+=p.radians(turningSpeed);
    }

    public void sensors(){
        // turn target angle into reading of a sensor
        for(int i=0;i<sensors.length;i++){
            if(i==4){ // special "ass" sensor which require special conditions
                if(targetAngle<sensors[i][0] || targetAngle>sensors[i][1]){
                    readingsPlayer[i] = 1;
                } else {
                    readingsPlayer[i] = 0;
                }
            } else // the rest of the sensors
                if(targetAngle<sensors[i][0] && targetAngle>sensors[i][1]){
                readingsPlayer[i] = 1;
            } else {
                readingsPlayer[i] = 0;
            }
        }

        // rays
        // collect readings from the rays and map them and store them in array
        //the point which is the most far away is 0 and the closest is 1
        for(int i = 0;i<rays.length;i++){
            rayReadings[i] = p.map(rays[i].dist,rays[i].raySight,0,0,1);
        }

    }

    void createRays(){
        //rays[0] = new Ray(p,position,p.radians(10),200);
        rays[0] = new Ray(p,position,p.radians(60),200);
        rays[1] = new Ray(p,position,p.radians(160),200);
        //rays[3] = new Ray(p,position,p.radians(-10),200);
        rays[2] = new Ray(p,position,p.radians(-60),200);
        rays[3] = new Ray(p,position,p.radians(-160),200);
    }

    void createSensors(){
        sensors[0][0] = p.radians(10);
        sensors[0][1] = p.radians(-10);
        sensors[1][0] = p.radians(-10);
        sensors[1][1] = p.radians(-30);
        sensors[2][0] = p.radians(-30);
        sensors[2][1] = p.radians(-60);
        sensors[3][0] = p.radians(-60);
        sensors[3][1] = p.radians(-160);
        sensors[4][0] = p.radians(-160);
        sensors[4][1] = p.radians(160);
        sensors[5][0] = p.radians(160);
        sensors[5][1] = p.radians(60);
        sensors[6][0] = p.radians(60);
        sensors[6][1] = p.radians(30);
        sensors[7][0] = p.radians(30);
        sensors[7][1] = p.radians(10);
    }

    public void checkForRoad(Polygon road){
        isOnRoad = road.contains(position.x,position.y);
    }


    public void updateTargetPos(PVector targetPos){
        this.targetPos = targetPos;
    }

    public void showInfo(){
        // everything about enemy
        p.fill(0);

        // global coordinate system
        p.pushMatrix();
        {
            p.translate(position.x,position.y);
            p.stroke(31, 10, 168);
            p.strokeWeight(1);
            p.line(-100, 0, 100, 0);
            p.line(0, -100, 0, 100);
            p.text("X", 90, 0);
        } p.popMatrix();

        // individual coordinate system
        p.pushMatrix();
        {
            p.translate(position.x,position.y);
            p.rotate(rotation);
            p.stroke(70, 151, 163);
            p.strokeWeight(1);
            p.line(-50, 0, 50, 0);
            p.line(0, -50, 0, 50);
            p.text("X", 40, 0);
        } p.popMatrix();

        // line to the target calculated from the angle of rotation
        p.pushMatrix();
        {
            p.translate(position.x,position.y);
            p.rotate(rotation);
            p.stroke(0,50);
            p.line(0,0,p.cos(targetAngle)*targetDist,p.sin(targetAngle)*targetDist);
        } p.popMatrix();

        // line to the closest bullet calculated from the angle of rotation
            p.pushMatrix();
            {
                p.translate(position.x, position.y);
                p.rotate(rotation);
                p.stroke(0);
                p.line(0, 0, p.cos(closestBulletAngle) * closestBulletDist, p.sin(closestBulletAngle) * closestBulletDist);
            }
            p.popMatrix();

        // rotations info
        p.pushMatrix();
        {
            p.fill(0);
            p.textSize(10);
            p.translate(position.x - 40,position.y + 30);
            p.text("ID: "+ index,0,0);
            p.text("Rotation: " + (int)p.degrees(rotation),0,10);
            p.text("Target Angle: " +(int)p.degrees(targetAngle),0,20);
            p.text("n: " +n,0,30);

            int active = 0;
            for(int i = 0; i< readingsPlayer.length; i++){
                if(readingsPlayer[i]>0){
                    active=i;
                }
            }
            p.text("Active sensor: "+ active,0,40);
            p.text("Dist traveled: "+ distTraveled,0,50);
            p.text("Is on road: "+ isOnRoad,0,60);
        } p.popMatrix();


    }

    public void showRoute(boolean display){
        p.strokeWeight(2);
        distTraveled = 0;
        for(int i =0;i<totalRoute.size();i++){
            if(i<totalRoute.size()-1){
                if(rounteOnRoad.contains(totalRoute.get(i))){
                    p.stroke(166, 23, 149);
                    distTraveled+=totalRoute.get(i).dist(totalRoute.get(i+1))/4;
                } else {
                    p.stroke(9, 111, 156);
                    distTraveled+=totalRoute.get(i).dist(totalRoute.get(i+1));
                }
                if(display) p.line(totalRoute.get(i).x,totalRoute.get(i).y,totalRoute.get(i+1).x,totalRoute.get(i+1).y);
            }
        }
    }

    public void showSensors(){
        //show work of sensors

        //p.noStroke();
        p.strokeWeight(1);
        p.stroke(0);

        p.pushMatrix();{
            p.translate(position.x,position.y);
            p.rotate(rotation);

            // for player

            for(int i = 0;i<sensors.length;i++){
                if(readingsPlayer[i]>0){
                    p.fill(0,40);
                } else {
                    p.fill(0,15);
                }
                p.triangle(0,0,p.cos(sensors[i][0])*80,p.sin(sensors[i][0])*80,p.cos(sensors[i][1])*80,p.sin(sensors[i][1])*80);

            }


        }p.popMatrix();
    }

    public void findClosestBullet(ArrayList<Projectile> bullets){
        // Find the closest projectile, it's position and angle.

        closestBulletDist = p.width;
        for(Projectile projectile:bullets){
            if(projectile.position.dist(position)<closestBulletDist){
                closestBulletDist = projectile.position.dist(position);
                closestBulletPos = projectile.position.copy();
                closestBulletAngle = calculateAngleToTarget(closestBulletPos) - rotation;
            }
            if(projectile.position.dist(position)<10){
                projectile.isDead = true;
                health-=1;
            }
        }

        if(bullets.size()==0){
            closestBulletDist = 0;
        }
    }



}
