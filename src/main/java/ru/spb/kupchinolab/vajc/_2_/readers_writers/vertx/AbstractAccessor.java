package ru.spb.kupchinolab.vajc._2_.readers_writers.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ru.spb.kupchinolab.vajc._2_.readers_writers.vertx.ActionType.RELEASE_RESOURCE;
import static ru.spb.kupchinolab.vajc._2_.readers_writers.vertx.ActionType.REQUEST_ACCESS;

abstract class AbstractAccessor extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private final String name;

    AbstractAccessor(String name) {
        this.name = name;
        log.info("{} was instantiated", name);
    }

    @Override
    public void start() {
        vertx.eventBus().consumer("start_topic", event -> {
            log.info("{} started", name);
            vertx.setTimer(getDelay(), new AccessHandler()); //emulating random access
        });
    }

    void access(AccessType accessType) {
        JsonObject requestAccess = new JsonObject()
                .put("name", name)
                .put("type", accessType)
                .put("action", REQUEST_ACCESS);
        log.info("{} is trying to get lock", name);
        vertx.eventBus().send("access_queue", requestAccess, event -> {
            if (event.succeeded()) {
                log.info("{} has got lock", name);
                long activeStart = System.currentTimeMillis();
                while (System.currentTimeMillis() <= activeStart + getDelay()) {
                    //DO NOTHING - emulating active work with resource
                }
                long nextDelay = getDelay();
                JsonObject releaseResource = new JsonObject()
                        .put("name", name)
                        .put("type", accessType)
                        .put("action", RELEASE_RESOURCE)
                        .put("nextDelay", nextDelay);
                log.info("{} is releasing lock", name);
                vertx.eventBus().send("access_queue", releaseResource, reply -> {
                    vertx.setTimer(nextDelay, new AccessHandler()); //emulating random access
                });
            } else {
                log.error("access request failed for {} with result {} and error {}", name, event.result());
            }
        });
    }

    private class AccessHandler implements Handler<Long> {

        @Override
        public void handle(Long event) {
            access();
        }


    }

    abstract protected void access();

    abstract protected long getDelay();
}
