package com.future.hackday.rest.ocrmatch;

import com.future.hackday.services.GoogleSearchClient;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/hackday")
public class FindURLsOfPagesWithTextRESTController {

    private GoogleSearchClient googleSearchClient = new GoogleSearchClient();

    @POST
    @Path("/text-to-search")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchTextInFuturePages (String textToSearch) {
        try {
            final Map<String, Double> map = googleSearchClient.searchOn(textToSearch);
            double maxScore = map.values().stream().max(Comparator.reverseOrder()).orElse(0.0);
            final SortedSetMultimap<Double, String> sortedRetMap = com.google.common.collect.TreeMultimap.create(Comparator.reverseOrder(), Comparator.naturalOrder());
            map.entrySet().forEach(e -> sortedRetMap.put(e.getValue(), e.getKey()));
            final String retUrl = sortedRetMap.get(maxScore).first();

            return Response.status(Response.Status.CREATED).entity(retUrl).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }


}
