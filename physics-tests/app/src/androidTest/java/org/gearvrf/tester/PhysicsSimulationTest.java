package org.gearvrf.tester;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import net.jodah.concurrentunit.Waiter;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBoxCollider;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRMeshCollider;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRSphereCollider;
import org.gearvrf.GVRTexture;
import org.gearvrf.physics.GVRRigidBody;
import org.gearvrf.physics.GVRWorld;
import org.gearvrf.physics.ICollisionEvents;
import org.gearvrf.unittestutils.GVRTestUtils;
import org.gearvrf.unittestutils.GVRTestableActivity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// kinematic equation
// d = 0.5*g(m/s^2)*t(s)^2
// (d/(0.5*g))^0.5 = t
// Xframes = t * 60
// tested on note4, no other apps running
// time to fall 2800ms or 168 frames, rounded up

@RunWith(AndroidJUnit4.class)
public class PhysicsSimulationTest {
    private GVRTestUtils gvrTestUtils;
    private Waiter mWaiter;
    GVRWorld world;

    private GVRAndroidResource cubeMesh = null;
    private GVRAndroidResource cubeTexture = null;

    private GVRAndroidResource sphereMesh = null;
    private GVRAndroidResource sphereTexture = null;

    @Rule
    public ActivityTestRule<GVRTestableActivity> ActivityRule = new
            ActivityTestRule<GVRTestableActivity>(GVRTestableActivity.class);

    @Before
    public void setUp() throws TimeoutException {
        mWaiter = new Waiter();

        GVRTestUtils.OnInitCallback initCallback = new GVRTestUtils.OnInitCallback() {
            @Override
            public void onInit(GVRContext gvrContext) {
                gvrContext.getMainScene().getMainCameraRig().getTransform().setPosition(0.0f, 6.0f, 0.0f);
                world = new GVRWorld(gvrContext);
                gvrContext.getMainScene().getRoot().attachComponent(world);
            }
        };

        gvrTestUtils = new GVRTestUtils(ActivityRule.getActivity(), initCallback);
        gvrTestUtils.waitForOnInit();

        try {
            cubeMesh = new GVRAndroidResource(gvrTestUtils.getGvrContext(), "cube.obj");
            cubeTexture = new GVRAndroidResource(gvrTestUtils.getGvrContext(), "cube.jpg");
            sphereMesh = new GVRAndroidResource(gvrTestUtils.getGvrContext(), "sphere.obj");
            sphereTexture = new GVRAndroidResource(gvrTestUtils.getGvrContext(), "sphere.jpg");
        } catch (IOException e) {

        }
    }

    @Test
    public void updatedOwnerTransformTest() throws Exception {
        addGroundMesh(gvrTestUtils.getMainScene(), 0.0f,0.0f,0.0f, 0.0f);

        GVRSceneObject[] objects = new GVRSceneObject[8];
        GVRRigidBody[] bodies = new GVRRigidBody[8];

        for(int i = 0; i < 8; i = i + 2) {
            GVRBoxCollider boxCollider = new GVRBoxCollider(gvrTestUtils.getGvrContext());
            boxCollider.setHalfExtents(0.5f, 0.5f, 0.5f);
            objects[i] = meshWithTexture("cube.obj", "cube.jpg");
            objects[i].getTransform().setPosition(0.0f -i, 6.0f, -10.0f - (2.0f*i));
            objects[i].attachCollider(boxCollider);
            bodies[i] = new GVRRigidBody(gvrTestUtils.getGvrContext());
            switch (i) {
                case 0 : bodies[i].setMass(0.0f); //object moves, rigid body doesn't
                         bodies[i].setSimulationType(GVRRigidBody.STATIC);
                         break;
                case 2 : bodies[i].setMass(1.0f);  //rigidbody is a "trigger  collider"
                         bodies[i].setSimulationType(GVRRigidBody.STATIC);
                         break;
                case 4 : bodies[i].setMass(0.0f);  //object moves, rigid body doesn't
                         bodies[i].setSimulationType(GVRRigidBody.KINEMATIC);
                         break;
                case 6 : bodies[i].setMass(100.0f); //rigid body is "sleeping", until it is hit by another
                         bodies[i].setSimulationType(GVRRigidBody.KINEMATIC);
                         break;
            }
            objects[i].attachComponent(bodies[i]);
            gvrTestUtils.getMainScene().addSceneObject(objects[i]);

            GVRSphereCollider sphereCollider = new GVRSphereCollider(gvrTestUtils.getGvrContext());
            sphereCollider.setRadius(0.5f);
            objects[i+1] = meshWithTexture("sphere.obj", "sphere.jpg");
            objects[i+1].getTransform().setScale(0.5f,0.5f,0.5f);
            objects[i+1].getTransform().setPosition(0.0f -i, 9.0f, -10.0f - (2.0f*i));
            objects[i+1].attachCollider(sphereCollider);
            bodies[i+1] = new GVRRigidBody(gvrTestUtils.getGvrContext(), 10.0f);
            objects[i+1].attachComponent(bodies[i+1]);
            gvrTestUtils.getMainScene().addSceneObject(objects[i+1]);

        }

        gvrTestUtils.waitForXFrames(160);

        for(int i = 0; i < 4; i++) {
            objects[i*2].getTransform().setPositionX(2.0f);
        }

        gvrTestUtils.waitForXFrames(160);

        float d = (bodies[1].getTransform().getPositionY()
                - bodies[0].getTransform().getPositionY()); //sphere is on top of the cube rigid body
        mWaiter.assertTrue(d > 0.5f);

        d = (bodies[3].getTransform().getPositionY()
                - bodies[2].getTransform().getPositionY()); //sphere went thru the cube
        mWaiter.assertTrue(d <= 0.5f);

        d = (bodies[5].getTransform().getPositionY()
                - bodies[4].getTransform().getPositionY()); //sphere is on top of the cube rigid body
        mWaiter.assertTrue(d > 0.5f);

        d = (bodies[7].getTransform().getPositionY()
                - bodies[6].getTransform().getPositionY()); //sphere fell of the cube
        mWaiter.assertTrue(d <= 0.5f);


        gvrTestUtils.waitForXFrames(1600);
    }

    @Test
    public void freeFallTest() throws Exception {
        CollisionHandler mCollisionHandler = new CollisionHandler();
        mCollisionHandler.extimatedTime = 2800; //... + round up
        mCollisionHandler.collisionCounter = 0;

        GVRSceneObject cube = addCube(gvrTestUtils.getMainScene(), 0.0f, 1.0f, -10.0f, 0.0f);
        GVRSceneObject sphere = addSphere(gvrTestUtils.getMainScene(), mCollisionHandler, 0.0f, 40.0f, -10.0f, 1.0f);

        mCollisionHandler.startTime = System.currentTimeMillis();
        gvrTestUtils.waitForXFrames(168 + 60);

        //Log.d("PHYSICS", "    Delta time of the last collisions:" + mCollisionHandler.lastCollisionTime);
        //mWaiter.assertTrue(mCollisionHandler.lastCollisionTime <= mCollisionHandler.extimatedTime);

        float d = (sphere.getTransform().getPositionY() - cube.getTransform().getPositionY()); //sphere is on top of the cube
        mWaiter.assertTrue( d <= 1.6f);
        //Log.d("PHYSICS", "    Number of collisions:" + mCollisionHandler.collisionCounter);
        mWaiter.assertTrue(mCollisionHandler.collisionCounter >= 1);

        gvrTestUtils.waitForXFrames(120);
    }

    @Test
    public void spacedFreeFallTest() throws Exception {
        OnTestStartRenderCallback beginCallback = new OnTestStartRenderCallback();
        gvrTestUtils.setOnRenderCallback(beginCallback);

        for(int i = 0; i < beginCallback.lenght; i++) {
            gvrTestUtils.waitForSceneRendering();
            gvrTestUtils.waitForXFrames(1);
        }
        gvrTestUtils.waitForSceneRendering();
        gvrTestUtils.setOnRenderCallback(null);

        beginCallback.mCollisionHandler.startTime = System.currentTimeMillis();
        gvrTestUtils.waitForXFrames(168);

        runTest(beginCallback.sphere, beginCallback.cube, beginCallback.lenght);

        //It is complicated to try to predict this timing depends on the device state, should be almost the same as the above
        //Log.d("PHYSICS", "    Delta time of the last collisions:" + beginCallback.mCollisionHandler.lastCollisionTime);
        //mWaiter.assertTrue(beginCallback.mCollisionHandler.lastCollisionTime <= beginCallback.mCollisionHandler.extimatedTime);

        //asserts can not happen on collision event, but we get an idea here
        //Log.d("PHYSICS", "    Number of collisions:" + beginCallback.mCollisionHandler.collisionCounter);
        mWaiter.assertTrue(beginCallback.mCollisionHandler.collisionCounter >= beginCallback.lenght);

        gvrTestUtils.waitForXFrames(120);
    }

    @Test
    public void simultaneousFreeFallTest() throws Exception {
        OnTestStartRenderAllCallback beginCallback = new OnTestStartRenderAllCallback(100);

        gvrTestUtils.setOnRenderCallback(beginCallback);
        gvrTestUtils.waitForSceneRendering();
        gvrTestUtils.setOnRenderCallback(null);

        beginCallback.mCollisionHandler.startTime = System.currentTimeMillis();
        gvrTestUtils.waitForXFrames(168 + 60 ); // Too many simultaneous events triggered need more time to process all???

        runTest(beginCallback.sphere, beginCallback.cube, beginCallback.lenght);

        //It is complicated to try to predict this timing
        //Log.d("PHYSICS", "    Delta time of the last collisions:" + mCollisionHandler.lastCollisionTime);
        //mWaiter.assertTrue(mCollisionHandler.lastCollisionTime <= mCollisionHandler.extimatedTime);

        //asserts can not happen on collision event, but we get an idea here
        //Log.d("PHYSICS", "    Number of collisions:" + beginCallback.mCollisionHandler.collisionCounter);
        mWaiter.assertTrue(beginCallback.mCollisionHandler.collisionCounter >= beginCallback.lenght);

        gvrTestUtils.waitForXFrames(120);
    }

    class OnTestStartRenderCallback implements GVRTestUtils.OnRenderCallback {
        public int lenght;
        public GVRSceneObject cube[];
        public GVRSceneObject sphere[];
        public CollisionHandler mCollisionHandler;
        int currentIndex;
        int k;

        OnTestStartRenderCallback () {
            cube = new GVRSceneObject[100];
            sphere  = new GVRSceneObject[100];
            mCollisionHandler = new CollisionHandler();
            mCollisionHandler.extimatedTime = 2800; //... + round up
            mCollisionHandler.collisionCounter = 0;
            currentIndex = 0;
            k = -100;
            lenght = 100;
        }

        @Override
        public void onSceneRendered() {
            k += 2;
            cube[currentIndex] = addCube(gvrTestUtils.getMainScene(), (float)k, 1.0f, -10.0f, 0.0f);
            sphere[currentIndex] = addSphere(gvrTestUtils.getMainScene(), mCollisionHandler, (float)k, 40.0f, -10.0f, 1.0f);
            currentIndex++;
        }
    }

    class OnTestStartRenderAllCallback implements GVRTestUtils.OnRenderCallback {
        public int lenght;
        public GVRSceneObject cube[];
        public GVRSceneObject sphere[];
        public CollisionHandler mCollisionHandler;
        private boolean objectsAdded = false;

        OnTestStartRenderAllCallback (int lenght) {
            cube = new GVRSceneObject[lenght];
            sphere  = new GVRSceneObject[lenght];
            mCollisionHandler = new CollisionHandler();
            mCollisionHandler.extimatedTime = 2800; //... + round up
            mCollisionHandler.collisionCounter = 0;
            this.lenght = lenght;
        }

        @Override
        public void onSceneRendered() {
            if (objectsAdded) {
                return;
            }

            objectsAdded = true;
            int x = -25;
            int j = 0;
            int k = 10;
            int z = -10;
            int step = 50 / k;

            world.setEnable(false);
            for(int i = 0; i < lenght; i++) {
                x += step;
                cube[i] = addCube(gvrTestUtils.getMainScene(), (float)x, 1.0f, (float)z, 0.0f);
                sphere[i] = addSphere(gvrTestUtils.getMainScene(), mCollisionHandler, (float)x, 40.0f, (float)z, 1.0f);
                j++;
                if (j == k) {
                    x = -25;
                    z -= 10;
                    j = 0;
                }
            }
            world.setEnable(true);
        }
    }

    public void runTest(GVRSceneObject sphere[], GVRSceneObject cube[], int lenght) throws  Exception{
        world.setEnable(false);
        for(int i = 0; i < lenght; i++) {
            float d = (sphere[i].getTransform().getPositionY() - cube[i].getTransform().getPositionY());
            //sphere is on top of the cube

            mWaiter.assertTrue( d <= 1.6f);
            mWaiter.assertTrue(sphere[i].getTransform().getPositionX() == cube[i].getTransform().getPositionX());
            mWaiter.assertTrue(sphere[i].getTransform().getPositionZ() == cube[i].getTransform().getPositionZ());
            //Log.d("PHYSICS", "    Index:" + i + "    Collision distance:" + d);
        }
    }

    public class CollisionHandler implements ICollisionEvents {
        public long startTime;
        public long extimatedTime;
        public long lastCollisionTime;
        public int collisionCounter;

        public void onEnter(GVRSceneObject sceneObj0, GVRSceneObject sceneObj1, float normal[], float distance) {
            lastCollisionTime = System.currentTimeMillis() - startTime;
            collisionCounter++;
        }

        public void onExit(GVRSceneObject sceneObj0, GVRSceneObject sceneObj1, float normal[], float distance) {
        }

    }

    private GVRSceneObject meshWithTexture(String mesh, String texture) {
        GVRSceneObject object = null;
        try {
            object = new GVRSceneObject(gvrTestUtils.getGvrContext(), new GVRAndroidResource(
                    gvrTestUtils.getGvrContext(), mesh), new GVRAndroidResource(gvrTestUtils.getGvrContext(),
                    texture));
        } catch (IOException e) {
            mWaiter.fail(e);
        }
        return object;
    }

    /*
     * Function to add a sphere of dimension and position specified in the
     * Bullet physics world and scene graph
     */
    private GVRSceneObject addSphere(GVRScene scene, ICollisionEvents mCollisionHandler, float x, float y, float z, float mass) {

        GVRSceneObject sphereObject = new GVRSceneObject(gvrTestUtils.getGvrContext(), sphereMesh, sphereTexture);

        sphereObject.getTransform().setScaleX(0.5f);
        sphereObject.getTransform().setScaleY(0.5f);
        sphereObject.getTransform().setScaleZ(0.5f);
        sphereObject.getTransform().setPosition(x, y, z);

        // Collider
        GVRSphereCollider sphereCollider = new GVRSphereCollider(gvrTestUtils.getGvrContext());
        sphereCollider.setRadius(0.5f);
        sphereObject.attachCollider(sphereCollider);

        // Physics body
        GVRRigidBody mSphereRigidBody = new GVRRigidBody(gvrTestUtils.getGvrContext());

        mSphereRigidBody.setMass(mass);
        sphereObject.getEventReceiver().addListener(mCollisionHandler);

        sphereObject.attachComponent(mSphereRigidBody);

        scene.addSceneObject(sphereObject);
        return sphereObject;
    }

    private GVRSceneObject addCube(GVRScene scene, float x, float y, float z, float mass) {

        GVRSceneObject cubeObject = new GVRSceneObject(gvrTestUtils.getGvrContext(), cubeMesh, cubeTexture);

        cubeObject.getTransform().setPosition(x, y, z);

        // Collider
        GVRBoxCollider boxCollider = new GVRBoxCollider(gvrTestUtils.getGvrContext());
        boxCollider.setHalfExtents(0.5f, 0.5f, 0.5f);
        cubeObject.attachCollider(boxCollider);

        // Physics body
        GVRRigidBody body = new GVRRigidBody(gvrTestUtils.getGvrContext());

        body.setMass(mass);

        cubeObject.attachComponent(body);

        scene.addSceneObject(cubeObject);
        return cubeObject;
    }

    private void addGroundMesh(GVRScene scene, float x, float y, float z, float mass) {
        try {
            GVRMesh mesh = gvrTestUtils.getGvrContext().createQuad(100.0f, 100.0f);
            GVRTexture texture =
                    gvrTestUtils.getGvrContext().loadTexture(new GVRAndroidResource(gvrTestUtils.getGvrContext(), "floor.jpg"));
            GVRSceneObject meshObject = new GVRSceneObject(gvrTestUtils.getGvrContext(), mesh, texture);

            meshObject.getTransform().setPosition(x, y, z);
            meshObject.getTransform().setRotationByAxis(-90.0f, 1.0f, 0.0f, 0.0f);

            // Collider
            GVRMeshCollider meshCollider = new GVRMeshCollider(gvrTestUtils.getGvrContext(), mesh);
            meshObject.attachCollider(meshCollider);

            // Physics body
            GVRRigidBody body = new GVRRigidBody(gvrTestUtils.getGvrContext());

            body.setRestitution(0.5f);
            body.setFriction(1.0f);

            meshObject.attachComponent(body);

            scene.addSceneObject(meshObject);
        } catch (IOException exception) {
            Log.d("gvrf", exception.toString());
        }
    }
}
