package de.uni_mannheim.informatik.dws.melt.matching_eval.tracks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.MAC;

/**
 * Developer note:
 * - This test requires a working internet connection.
 * - The SEALS servers must be online.
 * - While it might be desirable to test all tracks, note that in the testing step of the continuous integration pipeline, all test cases
 * are re-downloaded which significantly slows down the build process.
 * - The track repository is down.
 */
class TrackRepositoryTest {

    @Test
    //@EnabledOnOs({ MAC })
    public void testTracks(){
        // tests downloading process and implementation
        assertTrue(TrackRepository.Anatomy.Default.getTestCases().size() > 0);
    }

    @Test
    //@EnabledOnOs({ MAC })
    public void getMultifarmTrackForLanguage(){
        assertTrue(TrackRepository.Multifarm.getMultifarmTrackForLanguage("de").size() == 9);
        assertTrue(TrackRepository.Multifarm.getMultifarmTrackForLanguage("DE").size() == 9);
        assertTrue(TrackRepository.Multifarm.getMultifarmTrackForLanguage("en").size() == 9);
        assertTrue(TrackRepository.Multifarm.getMultifarmTrackForLanguage("EN").size() == 9);
        assertTrue(TrackRepository.Multifarm.getMultifarmTrackForLanguage("ENG").size() == 0);

        boolean appears = false;
        for(Track track : TrackRepository.Multifarm.getMultifarmTrackForLanguage("de")){
            if(track.getName().equals("de-en")) appears = true;
            assertFalse(track.getName().equals("ar-cn"));
        }
        assertTrue(appears, "The method does not return track de-en which should be contained when querying for 'de'.");
    }

    @Test
    //@EnabledOnOs({ MAC })
    public void getSpecificMultifarmTrack(){
        assertTrue(TrackRepository.Multifarm.getSpecificMultifarmTrack("de-en").getName().equals("de-en"));
        assertTrue(TrackRepository.Multifarm.getSpecificMultifarmTrack("de-EN").getName().equals("de-en"));
        assertTrue(TrackRepository.Multifarm.getSpecificMultifarmTrack("DE-EN").getName().equals("de-en"));
        assertNull(TrackRepository.Multifarm.getSpecificMultifarmTrack("ABCXYZ"));

        assertTrue(TrackRepository.Multifarm.getSpecificMultifarmTrack("de", "en").getName().equals("de-en"));
        assertTrue(TrackRepository.Multifarm.getSpecificMultifarmTrack("de", "EN").getName().equals("de-en"));
        assertTrue(TrackRepository.Multifarm.getSpecificMultifarmTrack("DE", "EN").getName().equals("de-en"));
        assertNull(TrackRepository.Multifarm.getSpecificMultifarmTrack("ABC", "XYZ"));
    }

    
    //@Test
    public void testMeltRepository(){
        assertEquals(1, TrackRepository.Anatomy.Default.getTestCases().size());
        assertEquals(21, TrackRepository.Conference.V1.getTestCases().size());
        assertEquals(5, TrackRepository.Knowledgegraph.V3.getTestCases().size());
        assertEquals(80, TrackRepository.IIMB.V1.getTestCases().size());
        assertEquals(2, TrackRepository.Biodiv.Default.getTestCases().size());
        assertEquals(11, TrackRepository.Link.Default.getTestCases().size());
    }
}