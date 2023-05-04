package search_engine;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import org.jsoup.Jsoup; // On a choisi d'utiliser la librairie Jsoup pour le scrapping
import org.jsoup.nodes.Document; // car elle est simple d'utilisation et efficace
import org.jsoup.nodes.Element; // et qu'elle permet de récupérer le contenu de balises HTML grace aux sélecteurs CSS
import org.jsoup.select.Elements;
import java.io.File;



public class VikidiaIndexation 
{
    public static void main(String[] args) 
    {
        try
        {

            //si le fichier liens.txt n'existe pas et qu'il n'est pas vide
            if(!Files.exists(Paths.get("resources/liens.txt")))
            {
                getUrlsFromVikidia();

            }
            else if(Files.size(Paths.get("resources/liens.txt")) == 0)
            {
                getUrlsFromVikidia();
            }
            /*
             *on récupère l'indice du plus grand fichier déjà créé
             * le but étant de ne pas réindexer les pages déjà indexées
             */
            int i = 0; // compteur pour le nombre de page déjà traitées
            File folder = new File("resources/INDEX_A_FROID");
            File[] listOfFiles = folder.listFiles();
    
            for (File file : listOfFiles) 
            {
                if (file.isFile()) 
                {
                    //on récupère le nom du fichier
                    String fileName = file.getName();
                    //on enlève l'extension .txt du nom du fichier
                    fileName = fileName.substring(0, fileName.length() - 4);
                    //on convertit le nom du fichier en entier
                    int index = Integer.parseInt(fileName);
                    //on récupère l'indice le plus grand
                    if (index > i)
                    {
                        i = index;
                    }
                }
            }
            System.out.println("Nombre de pages déjà traitées : " + i);
            i++;

            //On récupère les urls des articles à indexer dans un fichier texte
            List<String> urls = Files.readAllLines(Paths.get("resources/liens.txt"));

            //on crée un timer pour mesurer le temps d'execution du programme
            long startTime = System.nanoTime();

            //On parcourt la liste des urls (donc page à indexer)
            for (int j = i; j < urls.size(); j++ ) 
            {
                String url = urls.get(j);

                //On récupère le contenu des balises <p> de la page
                List<String> paragraphs = scrapBaliseP(url);

                //On crée un StringBuilder qui va contenir le contenu de la page
                StringBuilder siteParagraphs = new StringBuilder();

                //On parcourt la liste des paragraphes de la page
                for (String paragraph : paragraphs) 
                {
                    //On applique tous les traitements sur le paragraphe
                    paragraph = SearchEngine.allTreatments(paragraph);
                    //On ajoute le paragraphe au StringBuilder
                    siteParagraphs.append(paragraph).append(" ");
                }
                
                //On mesure le temps de traitemetn
                long endTime = System.nanoTime();

                //On calcule le temps d'execution en secondes
                long duration = (endTime - startTime)/1000000000;

                //On affiche le temps d'execution et le nombre de pages traitées
                System.out.println(url);
                System.out.println("Temps d'execution : " + duration + "s" + ", nombre de pages traitées : " + i);
                    
                //On écrit le contenu de la page dans un fichier texte
                BufferedWriter writer = Files.newBufferedWriter(Paths.get("resources/INDEX_A_FROID/"+i+".txt"));
                writer.write(url+"\n");

                String siteContent = siteParagraphs.toString().trim();
                //On crée un dictionnaire qui va contenir les mots et leur nombre d'occurences
                Map<String, Integer> wordOccurrences = compteMots(siteContent);
                //On écrit le dictionnaire dans le fichier texte
                for (Map.Entry<String, Integer> entry : wordOccurrences.entrySet()) 
                {
                    writer.write(entry.getKey() + ":" + entry.getValue() + "\n");
                }
                i++;
                writer.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

   
    public static void getUrlsFromVikidia()
    {
        try
        {
            int i=0;
            BufferedWriter writer = Files.newBufferedWriter(Paths.get("resources/liens.txt"));
            List<String> urls = new ArrayList<>();
            List<String> urlsSwitch = new ArrayList<>();
            urlsSwitch.add("https://fr.vikidia.org/w/index.php?title=Sp%C3%A9cial:Toutes_les_pages");
            while(i<urlsSwitch.size())
            {
                URL urlConnection = new URL(urlsSwitch.get(i));
                Document doc = Jsoup.parse(urlConnection, 3000);
                Element div = doc.select("div#mw-content-text").first();
                Elements links = div.select("a");
            
                for(Element link : links)
                {
                    
                    String url = link.attr("href");
                    if(url.startsWith("/wiki/"))
                    {
                        url = "https://fr.vikidia.org" + url;
                        if(!urls.contains(url))
                        {
                            writer.write(url+"\n");
                            urls.add(url);
                           
                        }
                        
                    }

                    else if(url.startsWith("/w/"))
                    {
                        url = "https://fr.vikidia.org" + url;
                        if(!urlsSwitch.contains(url))
                        {
                            urlsSwitch.add(url);
                        
                        }
                    }
                }
                i++;
            }
            writer.close();
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

       
        }

    
    //Méthode qui compte le nombre d'occurences de chaque mot dans un texte
    public static Map<String, Integer> compteMots(String input) 
    {
        //on crée un tableau de mots en enlevant les espaces en trop
        String[] mots = input.trim().split("\\s+");
        //on crée un dictionnaire qui va contenir les mots et leur nombre d'occurences
        Map<String, Integer> wordOccurrences = new HashMap<>();
        for (String mot : mots) 
        {
            mot = mot.toLowerCase(); //on met le mot en minuscule
            //on ajoute le mot au dictionnaire avec une occurence de 1 si le mot n'est pas encore dans le dictionnaire
            wordOccurrences.put(mot, wordOccurrences.getOrDefault(mot, 0) + 1);
        }
        return wordOccurrences;
    }
    // Fonction qui récupère le contenu des balises <p> d'une page web
    public static List<String> scrapBaliseP(String urlString) throws Exception {
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            content.append(line);
        }
        in.close();

        //on crée un pattern qui correspond à une balise <p> et on récupère le contenu de la balise
        Pattern pTagPattern = Pattern.compile("<p>(.*?)</p>", Pattern.DOTALL);
        //on crée un matcher qui va matcher le pattern sur le contenu de la page
        Matcher pTagMatcher = pTagPattern.matcher(content.toString());

        List<String> paragraphs = new ArrayList<>();
        while (pTagMatcher.find()) {
            String paragraph = pTagMatcher.group(1);
            String cleanedParagraph = enleveBalise(paragraph);
            paragraphs.add(cleanedParagraph);
        }
        paragraphs.remove(paragraphs.size() - 1);//on retire le dernier paragraphe qui contient des informations inutiles sur l'article
        return paragraphs;
    }


    //On enlève les balises html d'un paragraphe
    public static String enleveBalise(String input) {
        //on crée un pattern qui correspond à une balise html
        Pattern tagPattern = Pattern.compile("<[^>]*>");
        //on crée un matcher qui va matcher le pattern sur le paragraphe
        Matcher tagMatcher = tagPattern.matcher(input);
        //on remplace toutes les balises par des espaces
        return tagMatcher.replaceAll("");
    }

}