package org.zoneproject.extractor.plugin.wikimeta;

/*
 * #%L
 * ZONE-plugin-WikiMeta
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

import org.zoneproject.extractor.plugin.wikimeta.WikiMetaRequest;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.zoneproject.extractor.utils.Prop;

/**
 *
 * @author cdesclau
 */
public class WikiMetaRequestTest {
    private static final org.apache.log4j.Logger  logger = org.apache.log4j.Logger.getLogger(App.class);
    
    public WikiMetaRequestTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getProperties method, of class WikiMetaRequest.
     */
    @Test
    public void testGetProperties_String() {
        logger.info("getProperties");
        String texte = "Bienvenue à Antibes";
        ArrayList<Prop> result = WikiMetaRequest.getProperties(texte);
        logger.info(result);
        assertEquals(1,result.size());
        Prop p = new Prop("http://www.wikimeta.org/Entities#loc.admi","http://www.dbpedia.org/resource/Antibes",false);
        assertEquals(p,result.get(0));
    }

    /**
     * Test of getProperties method, of class WikiMetaRequest.
     */
    @Test
    public void testGetProperties_File() throws URISyntaxException {
        logger.info("getProperties");
        URI fileURI = getClass().getResource("/WikiMetaOutput_pip.json").toURI();
        ArrayList result = WikiMetaRequest.getProperties(new File(fileURI));
        assertEquals(4,result.size());
    }
    
    /**
     * Test of getProperties method, of class WikiMetaRequest.
     */
    @Test
    public void testGetProperties_File_2() throws URISyntaxException {
        logger.info("getProperties");
        URI fileURI = getClass().getResource("/WikiMetaOutput_mars.txt").toURI();
        ArrayList result = WikiMetaRequest.getProperties(new File(fileURI));
        ArrayList<Prop> expRes = new ArrayList<Prop>();
        expRes.add(new Prop("http://www.wikimeta.org/Entities#LOC","http://www.dbpedia.org/resource/Marseille",false));
        expRes.add(new Prop("http://www.wikimeta.org/Entities#ORG","http://www.dbpedia.org/resource/Le_Figaro",false));
        expRes.add(new Prop("http://www.wikimeta.org/Entities#LOC","Bouches-du-Rhône",true));

        assertEquals(expRes.size(), result.size());
        for(Prop p: expRes){
            assertTrue(result.contains(p));
        }
        
    }
    
    
    @Test
    public void testGetProperties_String_cleaningResult() {
        logger.info("getProperties");
        String texte = "Arnaud Montebourg est Arnaud Montebourg";
        ArrayList result = WikiMetaRequest.getProperties(texte);
        logger.info(result);
        assertEquals(1,result.size());
    }
    
    @Test
    public void testGetProperties_String_NullStrangeError(){
        logger.info("getProperties");
        String texte = " Sur la Toile, il se faisait appeler Jack Morg";
        ArrayList result = WikiMetaRequest.getProperties(texte);
        logger.info(result);
        ArrayList<Prop> expRes = new ArrayList<Prop>();
        expRes.add(new Prop("http://www.wikimeta.org/Entities#LOC","Toile",true));
        expRes.add(new Prop("http://www.wikimeta.org/Entities#PERS","Jack Morg",true));
        assertEquals(expRes.size(), result.size());
        for(Prop p: expRes){
            assertTrue(result.contains(p));
        }
        
    }
    
    @Test
    public void testGetnamedEntities_String_JSONError(){
        logger.info("getNamedEntities");
        String texte = "L'horloge de Windows 8 n'a pas toujours la bonne heure";
        ArrayList result = WikiMetaRequest.getProperties(texte);
        logger.info(result);
        ArrayList<Prop> expRes = new ArrayList<Prop>();
        expRes.add(new Prop("http://www.wikimeta.org/Entities#prod","http://www.dbpedia.org/resource/Windows_8",false));
        assertEquals(expRes.size(), result.size());
        for(Prop p: expRes){
            assertTrue(result.contains(p));
        }
    }
        
    @Test
    public void testGetnamedEntities_String_JSONError_2(){
        logger.info("getNamedEntities");
        String texte = "Online Learning Gets Massive, Open.  by Garry Kranz, Workforce Recruiting company Aquent is using a new twist on online learning to help its clients hire next-generation Web developers. Faced with job requests from companies that it could not fill, the Boston-based specialized recruiter for ad agencies in 2012 launched a massive open online course, or MOOC, on skills related to HTML5, [...]. by Garry Kranz, WorkforceRecruiting company Aquent is using a new twist on online learning to help its clients hire next-generation Web developers. Faced with job requests from companies that it could not fill, the Boston-based specialized recruiter for ad agencies in 2012 launched a massive open online course, or MOOC, on skills related to HTML5, the latest version of the markup language that defines how Internet content gets structured. Ad agencies need Web developers well-versed in mobile technologies such as HTML5, yet many code writers seem to lack the necessary skills to compete for available jobs, said Alison Farmer, Aquents vice president of learning and development. Even though unemployment was high, companies were telling us that most candidates werent qualified, Farmer said. We wondered: How do we take candidates that may have been competitive a year ago and help them acquire emerging skills?";
        ArrayList result = WikiMetaRequest.getProperties(texte);
        logger.info(result);
        assertEquals(16, result.size());
    }
    
    @Test
    public void testGetnamedEntities_String_JSONError_3(){
        logger.info("getNamedEntities");
        String texte = "l";
        ArrayList result = WikiMetaRequest.getProperties(texte);
        logger.info(result);
        assertEquals(0, result.size());
    }
    
    @Test
    public void testGetnamedEntities_String_JSONError_4(){
        logger.info("getNamedEntities");
        String texte = "Meuh.\n Parce";//rbelle.com Are farm-themed cookies on your to-do list? Then this cute. Parce que je suis normande et fière de l'êtreAre farm-themed cookies on your to-do list? Then this cute little cow cookie is just what you need. ";//Believe it0 commentaire(s)Votre nom (prénom ou pseudo)Votre emailMerci de recopier le texte de l'image ci-dessusRechercher :Le principe du siteLe site \"Un tour en cuisine\"       a pour vocation de présenter aux internautes des recettes       de cuisine venues de tous horizons. Deux thématiques sont abordées.Une fois par jour, sur le blog ,        une recette de cuisine est présentée.       Il s'agit d'une jolie recette qui comble autant les yeux que        les estomacs. On peut alors parler d'art culinaire.En parallèle du blog, des tours sont organisés pour favoriser le partage       entre blogueurs et la découverte de nouveaux blogs culinaires.       ";//Ces tours favorisent la découverte de recettes de tous les jours,       sans aucune obligation d'esthétique.Zoom sur le blogLe monde regorge de gens de talents.Chaque jour, à 21h, la photo d'une recette d'un autre blog est publiée       avec un lien vers l'article d'origine.        Il s'agit de découvrir le travail de quelqu'un        qui a concocté une recette originale et esthétique, une recette        à réaliser en famille, sous l\u0019oeil fasciné de vos enfants.Cela peut être des plantes fabriquées à partir de bonbons,        comme des animaux à partir de légumes.        Le mot clé de ce site est d'étonner et de donner envie de suivre        l'exemple de ces fabuleux cuisiniers dans la cuisine de Monsieur Madame        Tout Le Monde.Proposer votre recette !";
        ArrayList result = WikiMetaRequest.getProperties(texte);
        logger.info(result);
        assertEquals    (0,0);
        //assertEquals(0, result.size());
    }
}
