package com.anogaijin.colour.systems;

import com.anogaijin.colour.components.*;
import com.anogaijin.colour.physics.PhysicsUtil;
import com.anogaijin.colour.systems.states.CharacterState;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;

/**
 * Created by adunne on 2015/09/24.
 */
public class WalkingSystem extends IteratingSystem {
    ComponentMapper<RigidBody> rbm = ComponentMapper.getFor(RigidBody.class);
    ComponentMapper<KeyboardController> cm = ComponentMapper.getFor(KeyboardController.class);
    ComponentMapper<Motion> mm = ComponentMapper.getFor(Motion.class);
    ComponentMapper<Brain> bm = ComponentMapper.getFor(Brain.class);
    ComponentMapper<Walk> wm = ComponentMapper.getFor(Walk.class);
    ComponentMapper<CharacterSensor> sm = ComponentMapper.getFor(CharacterSensor.class);

    public WalkingSystem() {
        super(Family.all(Walk.class, RigidBody.class, Motion.class, KeyboardController.class, Brain.class, CharacterSensor.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        RigidBody rigidBody = rbm.get(entity);
        KeyboardController controller = cm.get(entity);
        Motion motion = mm.get(entity);
        Walk walk = wm.get(entity);
        Brain brain = bm.get(entity);
        CharacterSensor sensors = sm.get(entity);

        // If we've fallen hand this off to another state
        //
        if (!sensors.bottomIsTouching) {
            brain.movement.changeState(CharacterState.Falling);

            return;
        }

        if (Gdx.input.isKeyPressed(controller.RIGHT)) {
            motion.velocity.set(walk.terminalVelocity, motion.velocity.y);

            brain.movement.changeState(CharacterState.Walking);

            if (rigidBody.flipped)
                flipRigidBody(rigidBody);
        }
        else if (Gdx.input.isKeyPressed(controller.LEFT)) {
            motion.velocity.set(-walk.terminalVelocity, motion.velocity.y);

            brain.movement.changeState(CharacterState.Walking);

            if (!rigidBody.flipped)
                flipRigidBody(rigidBody);
        }
        else {
            motion.velocity.set(0f, motion.velocity.y);

            brain.movement.changeState(CharacterState.Idle);
        }

        // Calculates required force to reach desired velocity in a given time-step
        //
        motion.force.x = PhysicsUtil.calculateRequiredForce(
                rigidBody.body.getMass(),
                motion.velocity.x,
                rigidBody.body.getLinearVelocity().x,
                1 / 60f);

        if (motion.force.x == 0f)
            return;

        // Apply force!
        //
        rigidBody.body.applyForceToCenter(motion.force.x, 0f, true);
    }

    private void flipRigidBody(RigidBody rigidBody) {
        rigidBody.flipped = !rigidBody.flipped;

        for (Fixture fixture : rigidBody.body.getFixtureList()) {
            // We don't need to flip the capsule
            //
            BoundingBoxAttachment attachment = (BoundingBoxAttachment)fixture.getUserData();
            if (attachment.getName().endsWith("capsule"))
                continue;

            PolygonShape polygon = (PolygonShape)fixture.getShape();
            Vector2 tmpVector = new Vector2();
            for (int i = 0; i < polygon.getVertexCount(); i++) {
                polygon.getVertex(i, tmpVector);
                tmpVector.set(-tmpVector.x, tmpVector.y);
            }
        }
    }
}