package net.java.pathfinder.api;

import fish.payara.micro.cdi.Inbound;
import fish.payara.micro.cdi.Outbound;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import net.java.pathfinder.api.reactive.GraphTraversalRequest;
import net.java.pathfinder.api.reactive.GraphTraversalResponse;
import net.java.pathfinder.internal.GraphDao;

@Stateless
@Path("/graph-traversal")
public class GraphTraversalService {

    @Inject
    private GraphDao dao;
    private final Random random = new Random();
    private static final long ONE_MIN_MS = 1000 * 60;
    private static final long ONE_DAY_MS = ONE_MIN_MS * 60 * 24;

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    @Inject
    @Outbound
    private Event<GraphTraversalResponse> responseEvent;
    
    @Inject
    private CacheManager cacheManager;

    @GET
    @Path("/shortest-path")
    @Produces({"application/json", "application/xml; qs=.75"})
    // TODO Add internationalized messages for constraints.
    public void findShortestPath(
            @Suspended AsyncResponse response,
            @NotNull @Size(min = 5, max = 5) @QueryParam("origin") String originUnLocode,
            @NotNull @Size(min = 5, max = 5) @QueryParam("destination") String destinationUnLocode,
            @QueryParam("deadline") String deadline) throws InterruptedException {

        List<TransitPath> candidates = new ArrayList<>();

        findShortestPath(originUnLocode, destinationUnLocode,
                candidates::add,
                () -> response.resume(new GenericEntity<List<TransitPath>>(candidates) {
                }),
                e -> response.resume(e));
    }

    public void findShortestPath(@Observes @Inbound GraphTraversalRequest request) {

        final Cache cache = getAtMostOnceDeliveryCache();
        if (cache != null && cache.remove(request.getId())) {
            logger.info("Received a computation request, id=" + request.getId());

            String originUnLocode = request.getOrigin();
            String destinationUnLocode = request.getDestination();
            findShortestPath(originUnLocode, destinationUnLocode,
                    item -> responseEvent.fire(GraphTraversalResponse.newWithValue(item, request)),
                    () -> responseEvent.fire(GraphTraversalResponse.newCompleted(request)),
                    e -> responseEvent.fire(GraphTraversalResponse.newCompletedWithException(e, request)));

        } else {
            logger.info("Ignoring computation request, somebody else is computing, id=" + request.getId());
        }
    }

    private Cache<Long, String> getAtMostOnceDeliveryCache() {
        return cacheManager.getCache("GraphTraversalRequest");
    }

    public void findShortestPath(String originUnLocode,
            String destinationUnLocode, Consumer<TransitPath> onNewItem, Runnable onCompleted, Consumer<Throwable> onException) {
        Date date = nextDate(new Date());

        try {
            List<String> allVertices = dao.listLocations();
            allVertices.remove(originUnLocode);
            allVertices.remove(destinationUnLocode);

            int candidateCount = getRandomNumberOfCandidates();

            for (int i = 0; i < candidateCount; i++) {
                allVertices = getRandomChunkOfLocations(allVertices);
                List<TransitEdge> transitEdges = new ArrayList<>(
                        allVertices.size() - 1);
                String firstLegTo = allVertices.get(0);

                Date fromDate = nextDate(date);
                Date toDate = nextDate(fromDate);
                date = nextDate(toDate);

                transitEdges.add(new TransitEdge(
                        dao.getVoyageNumber(originUnLocode, firstLegTo),
                        originUnLocode, firstLegTo, fromDate, toDate));

                for (int j = 0; j < allVertices.size() - 1; j++) {
                    String current = allVertices.get(j);
                    String next = allVertices.get(j + 1);
                    fromDate = nextDate(date);
                    toDate = nextDate(fromDate);
                    date = nextDate(toDate);
                    transitEdges.add(new TransitEdge(dao.getVoyageNumber(current,
                            next), current, next, fromDate, toDate));
                }

                String lastLegFrom = allVertices.get(allVertices.size() - 1);
                fromDate = nextDate(date);
                toDate = nextDate(fromDate);
                transitEdges.add(new TransitEdge(
                        dao.getVoyageNumber(lastLegFrom, destinationUnLocode),
                        lastLegFrom, destinationUnLocode, fromDate, toDate));

                Thread.sleep(Integer.valueOf(System.getProperty("reactivejavaee.itemslowdown", "3000")));

                onNewItem.accept(new TransitPath(transitEdges));
            }

            onCompleted.run();

            logger.info("Path Finder Service called for " + originUnLocode + " to " + destinationUnLocode);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            onException.accept(e);
        }
    }

    private Date nextDate(Date date) {
        return new Date(date.getTime() + ONE_DAY_MS
                + (random.nextInt(1000) - 500) * ONE_MIN_MS);
    }

    private int getRandomNumberOfCandidates() {
        return 3 + random.nextInt(3);
    }

    private List<String> getRandomChunkOfLocations(List<String> allLocations) {
        Collections.shuffle(allLocations);
        int total = allLocations.size();
        int chunk = total > 4 ? 1 + new Random().nextInt(5) : total;
        return allLocations.subList(0, chunk);
    }
}
