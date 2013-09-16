package org.zoneproject.extractor.utils;

/*
 * #%L
 * ZONE-utils
 * %%
 * Copyright (C) 2012 ZONE-project
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.FileManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory; 

/**
 *
 * @author Desclaux Christophe <christophe@zouig.org>
 */

public abstract class VirtuosoDatabase {
    private static final org.apache.log4j.Logger  logger = org.apache.log4j.Logger.getLogger(App.class);
    private static Model st = null;
    private static String VIRTUOSO_SERVER = Config.getVar("Virtuoso-server-uri");
    //private static String VIRTUOSO_SERVER = "jdbc:virtuoso://localhost:12346";
    private static String VIRTUOSO_USER = Config.getVar("Virtuoso-server-user");
    private static String VIRTUOSO_PASS = Config.getVar("Virtuoso-server-pass");
    public static String ZONE_URI = ZoneOntology.GRAPH_NEWS;
    
    public static Model getStore(){
        if(st == null){
            VirtGraph vgraph = new VirtGraph(VIRTUOSO_SERVER, VIRTUOSO_USER, VIRTUOSO_PASS);
            vgraph.setReadFromAllGraphs(true);
            st = new VirtModel(vgraph);
        }
        return st;
    }
    
    public static Model getStore(String graph){
        return VirtModel.openDatabaseModel(graph, VIRTUOSO_SERVER, VIRTUOSO_USER, VIRTUOSO_PASS);
    }
    
    public static void addItems(ArrayList<Item> items){
        addItems(items.toArray(new Item[items.size()]));
    }
    
    public static void addItems(Item[] items){        
        for(int i=0; i < items.length;i++){
            addItem(items[i]);
        }
    }

    public static void addItem(Item item){
        addAnnotations(item.getUri(), item.getElements());
    }
    
    public static void addAnnotations(String itemUri, ArrayList<Prop> props){
        for (Iterator<Prop> it = props.iterator(); it.hasNext();) {
            Prop prop = it.next();
            addAnnotation(itemUri, prop);
        }
    }
    
    /**
     * Add annotation for a news
     * @param itemUri
     * @param prop 
     */
    public static void addAnnotation(String itemUri, Prop prop){
        int i = 5;
        while((i--)>0){
            try{
                addAnnotation(itemUri, prop, ZoneOntology.GRAPH_NEWS);
                i=0;
            }catch (com.hp.hpl.jena.shared.JenaException ex){
                logger.warn("annotation process error because of virtuoso partial error "+itemUri);
                try{Thread.currentThread().sleep(1000);}catch(InterruptedException ie){}
            }
            
        }
        
    }
    
    /**
     * Add annotation for an item of any graph
     * @param itemUri
     * @param prop
     * @param graph 
     */
    public static void addAnnotation(String itemUri, Prop prop, String graph){
        try {
            if(prop.isIsSearchable()) {
                VirtuosoDatabase.addAnnotation(prop.getType().getURI(),new Prop(ZoneOntology.ANNOTATION, "true",true));
            }
            Model model = ModelFactory.createDefaultModel();
            Resource itemNode = model.createResource(itemUri);
            String val = prop.getValue();
            if(val == null)
                val = "";
            byte[] utf8;
            utf8 = val.getBytes("UTF-8");
            val = new String(utf8, "UTF-8");
            if(prop.isLiteral()){
                itemNode.addLiteral(prop.getType(), model.createLiteral(val));
            }
            else{
                itemNode.addProperty(prop.getType(), model.createResource(val));
            }
            getStore(graph).add(model);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(VirtuosoDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Run a SPARQL request on the EndPoint
     * @param queryString the SPARQL request
     * @return the set of results
     */
    public static ResultSet runSPARQLRequest(String queryString){
        return VirtuosoQueryExecutionFactory.create(queryString,getStore()).execSelect() ;
    }
    
    /**
     * Run a SPARQL request on the EndPoint
     * @param queryString the SPARQL request
     * @param graphUri the Graph in which work
     * @return the set of results
     */
    public static ResultSet runSPARQLRequest(String queryString, String graphUri){
        return VirtuosoQueryExecutionFactory.create(queryString,getStore(graphUri)).execSelect() ;
    }
    public static boolean runSPARQLAsk(String queryString){
        return VirtuosoQueryExecutionFactory.create(queryString,getStore()).execAsk() ;
    }
    
    public static ResultSet getRelationsForURI(String uri, String graphUri){
        String query = "SELECT DISTINCT ?relation ?object { <"+uri+"> ?relation ?object.}";
        return runSPARQLRequest(query,graphUri);
    }
    /**
     * Get a map of concepts/objects for a particular Uri
     * @param uri
     * @param graphUri
     * @return 
     */
    public static Map getMapForURI(String uri, String graphUri){
        ResultSet results = Database.getRelationsForURI(uri, graphUri);
        Map<String,String> res = new HashMap<String,String>();
        
        while (results.hasNext()) {
            QuerySolution result = results.nextSolution();
            res.put(result.get("?relation").toString(),result.get("?object").toString() );
        }
        return res;
    }

    /**
     * get all items which has not been annotated for a plugin
     * @param pluginURI the plugin URI
     * @return the items
     */
    public static Item[] getItemsNotAnotatedForOnePlugin(String pluginURI){
        return getItemsNotAnotatedForOnePlugin(pluginURI,100);
    }
    public static Item[] getItemsNotAnotatedForOnePlugin(String pluginURI, int limit){
        ArrayList<Item> items = new ArrayList<Item>();
        String request = "SELECT DISTINCT ?uri  FROM <http://zone-project.org/datas/items> WHERE{  ?uri <http://purl.org/rss/1.0/title> ?title  OPTIONAL {?uri <"+pluginURI+"> ?pluginDefined} FILTER (!bound(?pluginDefined)) } LIMIT "+limit;
        ResultSet results = runSPARQLRequest(request);

        while (results.hasNext()) {
            QuerySolution result = results.nextSolution();
            try{
                items.add(getOneItemByURI(result.get("?uri").toString()));
            }catch(com.hp.hpl.jena.shared.JenaException e){
                Logger.getLogger(VirtuosoDatabase.class.getName()).log(Level.WARNING, null, e);
                getStore(ZoneOntology.GRAPH_NEWS).removeAll(ResourceFactory.createResource(result.get("?uri").toString()),null,null);
            }
        }
        return items.toArray(new Item[items.size()]);
    }

    /**
     * get all items which has not been annotated for a plugin
     * @param pluginURI the plugin URI
     * @return the items
     */
    public static Item[] getItemsNotAnotatedForPluginsWithDeps(String pluginURI, String []deps){
        return getItemsNotAnotatedForPluginsWithDeps(pluginURI, deps,10);
    }
    /**
     * get all items which has not been annotated for a plugin
     * @param pluginURI the plugin URI
     * @return the items
     */
    public static Item[] getItemsNotAnotatedForPluginsWithDeps(String pluginURI, String []deps, int limit){
        ArrayList<Item> items = new ArrayList<Item>();
        String requestPlugs ="";
        int i=0;
        for(String curPlugin : deps){
            requestPlugs += ". ?uri <"+curPlugin+"> ?deps"+i++ +" ";
        }
        
        String request = "SELECT ?uri WHERE{  ?uri <http://purl.org/rss/1.0/title> ?title "+requestPlugs+". OPTIONAL {?uri <"+pluginURI+"> ?pluginDefined.  } FILTER (!bound(?pluginDefined)) } LIMIT "+limit;
        ResultSet results = runSPARQLRequest(request);

        while (results.hasNext()) {
            QuerySolution result = results.nextSolution();
            items.add(getOneItemByURI(result.get("?uri").toString()));
        }
        return items.toArray(new Item[items.size()]);
    }

    /**
     * get all items which has not been annotated for a plugin
     * @param pluginURI the plugin URI
     * @return the items
     */
    public static ArrayList<Item> getItemsFromSource(String source){
        ArrayList<Item> items = new ArrayList<Item>();
        String requestPlugs ="";
        
        String request = "SELECT DISTINCT(?uri) WHERE{?uri <http://purl.org/rss/1.0/source> <"+source+">.}";
        ResultSet results = runSPARQLRequest(request);

        while (results.hasNext()) {
            QuerySolution result = results.nextSolution();
            items.add(getOneItemByURI(result.get("?uri").toString()));
        }
        return items;
    }

    /**
     * Get an Item from the Database
     * @param uri
     * @return 
     */
    public static Item getOneItemByURI(String uri){
        String request = "SELECT ?relation ?value FROM <http://zone-project.org/datas/items> WHERE{  <"+uri+"> ?relation ?value}";
        ResultSet results = runSPARQLRequest(request);
        return new Item(uri,results,uri,"relation","?value");
    }

    public static boolean ItemURIExist(String uri){
        return contains(uri, "http://purl.org/rss/1.0/title");
    }
    
    public static boolean contains(String uri, String prop){
        Model m = ModelFactory.createDefaultModel();
        Resource r = m.createResource(uri);
        Property p = m.createProperty(prop);
        boolean res = true;
        try{
            res = getStore().contains(r,p);
        }
        catch(Exception e){
            deleteItem(uri);
            logger.warn("The item "+uri +" existance cannot be check due to encoding errors  ("+ e+")");
            return false;
        }
        return res;
        
    }
    
    public static void verifyItemsList(ArrayList<Item> items){
        for (Iterator<Item> iterator = items.iterator(); iterator.hasNext(); ) {
            Item o = iterator.next();
            if (ItemURIExist(o.getUri())) {
                iterator.remove();
            }
        }
    }
    
    public static void deleteItem(String uri){
        try{
            Item item = getOneItemByURI(uri);
            getStore(ZoneOntology.GRAPH_NEWS).remove(item.getModel());
        }catch(com.hp.hpl.jena.shared.JenaException ex){
            String deleteRequest="DELETE{<"+uri+"> ?a ?b.}WHERE{<"+uri+"> ?a ?b.}";
            runSPARQLRequest(deleteRequest,ZoneOntology.GRAPH_NEWS);
        }
    }
    
    public static void loadFolder(String graphURI,String dir){
        File file = new File(dir);
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() == true) {
                    try {
                        loadFile(graphURI,files[i].getAbsolutePath());
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(VirtuosoDatabase.class.getName()).log(Level.WARNING, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(VirtuosoDatabase.class.getName()).log(Level.WARNING, null, ex);
                    }
                } else if(files[i].isDirectory() == true){
                    loadFolder(graphURI,files[i].getAbsolutePath());
                }
            }
        }
    }
    
    public static void loadFile(String graphURI,String path) throws FileNotFoundException, IOException{
        Model model = ModelFactory.createDefaultModel();
        FileManager.get().readModel(model,path);
        getStore(graphURI).add(model, true);
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException{
        /*
        loadFile("","./test.rdf");
        ResultSet r = runSPARQLRequest("SELECT ?x ?t WHERE {?x rdf:type ?t} ");
        logger.info(r.getResourceModel());
        
        logger.info("addItem");
        String uri="http://testURI.com/#MyURI";
        Item item = new Item(uri);
        item.addProp(new Prop("http://purl.org/rss/1.0/title","le titre",true));
        
        
        VirtuosoDatabase.addItem(item);
        VirtuosoDatabase.deleteItem(uri);
        logger.info(VirtuosoDatabase.ItemURIExist(uri));
        logger.info(VirtuosoDatabase.ItemURIExist("http://www.personnes.com#Margot"));
        * */
        //extractDB();
        deleteItem("http://www.leparisien.fr/politique/report-de-la-taxe-diesel-noel-mamere-menace-de-quitter-eelv-16-09-2013-3141815.php");
        System.out.println(getItemsNotAnotatedForOnePlugin("http://zone-project.org/model/plugins/WikiMeta"));
        System.out.println(VIRTUOSO_SERVER);
        System.out.println(getOneItemByURI("https://twitter.com/SbayAlticus/status/352411307980500992aaaa"));
        }
    
    public static void extractDB(){
        FileOutputStream fout;
        try {
            fout = new FileOutputStream("dbExtract_monde.rdf");
            //CONSTRUCT{?uri ?prop ?value} WHERE {  ?uri <http://purl.org/rss/1.0/source> <http://www.tv5.org/TV5Site/rss/actualites.php?rub=12>. ?uri ?prop ?value}
            
            String queryString ="CONSTRUCT{?uri ?prop ?source} WHERE {  ?uri <http://purl.org/rss/1.0/source> ?source}";
            //Query query = QueryFactory.create(queryString) ;
            QueryExecution qexec = QueryExecutionFactory.create(queryString, getStore()) ;
            Model results = qexec.execConstruct() ;
            results.write(System.out);
            //results.write(fout, "RDF/XML");
            //ResultSetFormatter.out(System.out, results, query) ;

            
            //ResultSet r =VirtuosoQueryExecutionFactory.create(queryString,getStore()).execSelect();
            //Model m = ModelFactory.createDefaultModel();
            //Model m = ResultSetFormatter.toModel(results) ;
            //m.write(fout);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(VirtuosoDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
}
}
