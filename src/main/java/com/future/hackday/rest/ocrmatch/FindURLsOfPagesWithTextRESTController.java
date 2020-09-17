package com.future.hackday.rest.ocrmatch;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class FindURLsOfPagesWithTextRESTController {

    @POST
    @Path("/text-to-search")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchTextInFuturePages (String textToSearch) {

        try {

        } catch (IllegalStateException e) {
        }
        return Response.status(Response.Status.CREATED).build();
    }
}
