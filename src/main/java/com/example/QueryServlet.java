package com.example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/QueryServlet")
public class QueryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get parameters from request
        String repoId = request.getParameter("repoId");
        String query = request.getParameter("query");

        // Construct the SPARQL endpoint URL
        String sparqlEndpoint = "http://localhost:2525/rdf4j-server/repositories/" + repoId;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Repository repository = new SPARQLRepository(sparqlEndpoint);
            repository.initialize();

            try (RepositoryConnection connection = repository.getConnection()) {
                Query sparqlQuery = connection.prepareQuery(QueryLanguage.SPARQL, query);
                JSONObject jsonResponse = new JSONObject();

                if (sparqlQuery instanceof TupleQuery) {
                    TupleQueryResult result = ((TupleQuery) sparqlQuery).evaluate();
                    JSONArray resultsArray = new JSONArray();

                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        JSONObject row = new JSONObject();
                        for (String name : result.getBindingNames()) {
                            Value value = bindingSet.getValue(name);
                            row.put(name, value != null ? value.stringValue() : JSONObject.NULL);
                        }
                        resultsArray.put(row);
                    }
                    jsonResponse.put("results", resultsArray);
                    result.close();

                } else if (sparqlQuery instanceof BooleanQuery) {
                    boolean result = ((BooleanQuery) sparqlQuery).evaluate();
                    jsonResponse.put("result", result);
                }

                out.print(jsonResponse.toString());
            }
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", e.getMessage());
            out.print(errorResponse.toString());
        }
    }
}
