package de.uni_mannheim.informatik.dws.melt.matching_owlapi;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache for ontologies for the OWL Api.
 */
public class OntologyCacheOwlApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyCacheOwlApi.class);

    /**
     * The internal cache for ontologies that is dependent on the OntModelSpec.
     */
    private static Map<String, OWLOntology> ontologyCache = new HashMap<>();

    /**
     * This flag indicates whether the cache is to be used (i.e., ontologies are held in memory).
     */
    private static boolean isDeactivatedCache = false;

    /**
     * Returns the OntModel for the given uri using a cache if indicated to do so.
     * @param uri The URI of the ontology that shall be cached.
     * @param useCache Indicates whether the cache shall be used. If set to false, ontologies will not be held in memory but re-read every time time.
     * @return OntModel reference.
     */
    public static synchronized OWLOntology get(String uri, boolean useCache) {
        if (useCache) {
            OWLOntology model = ontologyCache.get(uri);            
            if (model == null) {
                // model not found in cache → read, put it there and return
                LOGGER.info("Reading model into cache (" + uri + ")");
                model = readOWLOntology(uri);
                if(!isDeactivatedCache) {
                    ontologyCache.put(uri, model);
                }
                return model;                
            } else {
                LOGGER.info("Returning model from cache.");
                return model;
            }
        } else {
            // → do not use cache
            // plain vanilla case: read ontology and return
            return readOWLOntology(uri);
        }
    }
    private static OWLOntology readOWLOntology(String uri){
        OWLOntologyManager man = createManager();
        try {
            return man.loadOntologyFromOntologyDocument(IRI.create(uri));
        } catch (OWLOntologyCreationException ex) {
            LOGGER.warn("Cannot read OWLOntology of URI " + uri + ". Returning empty ontology.", ex);
            try {
                return man.createOntology();
            } catch (OWLOntologyCreationException ex1) {
                LOGGER.warn("Cannot create empty ontology. Should not happen...", ex1);
                return null;
            }
        }        
    }
    
    private static OWLOntologyManager createManager(){
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        
        //in case the ontology import another ontology which does not work, ignore silently
        //see issue https://github.com/owlcs/owlapi/issues/503
        //same as in https://github.com/ernestojimenezruiz/logmap-matcher/blob/2f139a5a0bcc9377dd5744155af39d85d6dec205/src/main/java/uk/ac/ox/krr/logmap2/OntologyLoader.java#L117
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        config = config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        man.setOntologyLoaderConfiguration(config);
        return man;
    }
    
    public static OWLOntology get(URL url, boolean useCache) {
        return get(url.toString(), useCache);
    }
  
    public static OWLOntology get(String uri){
        return get(uri, true);
    }
    
    public static OWLOntology get(URL url){
        return get(url, true);
    }
    
    public static OWLOntology get(File file){
        return get(file.toURI().toString(), true);
    }

    public boolean isDeactivatedCache() {
        return isDeactivatedCache;
    }

    /**
     * Empties the cache.
     */
    public static void emptyCache() {
        ontologyCache = new HashMap<>();
    }

    /**
     * Deactivating the cache will also clear the cache.
     * If an ontology is requested twice it is ready every time from disk.
     * @param deactivatedCache true if cache is to be deactivated, else false.
     */
    public void setDeactivatedCache(boolean deactivatedCache) {
        if(deactivatedCache){
            emptyCache();
        }
        isDeactivatedCache = deactivatedCache;
    }
}
