package de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.knowledge.services.persistence;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceServiceTest {

    @Test
    @Disabled
    void testPreconfiguredPersistences(){
        for(PersistenceService.PreconfiguredPersistences persistence :  PersistenceService.PreconfiguredPersistences.values() ){
            assertNotNull(persistence.getFilePath());
            assertNotNull(persistence.getKeySerializer());
            assertNotNull(persistence.getValueSerializer());
            assertNotNull(persistence.getKeyClass());
        }
    }

}