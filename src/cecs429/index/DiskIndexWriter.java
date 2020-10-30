package cecs429.index;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Writes index to disk
 */
public class DiskIndexWriter {

    String path;
    public DiskIndexWriter(String path){
        this.path = path;
    }

    /**
     * Writes the
     * @param index to Postings.bin file and
     * creates a mapping between term and its address in database positionalIndex.db's vocabToAddress collection
     * @return list of addresses where sorted terms are stored
     */
    public List<Long> writeIndex(Index index){
        List<Long> locations = new LinkedList<>();

        File postingsFile = new File(path,"Postings.bin");
        File mapDBFile = new File(path,"positionalIndex.db");
        postingsFile.getParentFile().mkdirs();
        if(postingsFile.exists()) {
            postingsFile.delete();
            mapDBFile.delete();
        }
        Map<String, List<Posting>> positionalInvertedIndex = index.getIndex();
        List<String> sortedTerms = new ArrayList<>(index.getIndex().keySet());
        Collections.sort(sortedTerms);
        DB db = DBMaker
                .fileDB(path+File.separator+"positionalIndex.db")
                .fileMmapEnable()
                .make();
        ConcurrentMap<String,Long> diskIndex = db
                .hashMap("vocabToAddress", Serializer.STRING, Serializer.LONG)
                .create();

        try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(postingsFile))){
            postingsFile.createNewFile();
            for(String term: sortedTerms){
                List<Posting> postingList = positionalInvertedIndex.get(term);
                //Writing current stream location to output list and dictionary of term to address
                locations.add((long)outputStream.size());
                diskIndex.put(term, (long)outputStream.size());
                //Writing Number of postings for given term; dft
                outputStream.writeInt(postingList.size());
                int previousDocID=0;
                for(Posting posting:postingList){
                    //Writing gap of document ID of a posting; d
                    outputStream.writeInt(posting.getDocumentId()-previousDocID);
                    //Writing weight of @term, @posting's document Id; wdt
                    outputStream.writeDouble(1+Math.log(posting.getPositions().size()));
                    previousDocID=posting.getDocumentId();
                    //Writing Number of positions for given posting; tftd
                    outputStream.writeInt(posting.getPositions().size());
                    int previousPosition=0;
                    for(Integer position:posting.getPositions()){
                        //Writing gap of positions of posting; p
                        outputStream.writeInt(position-previousPosition);
                        previousPosition=position;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        db.close();
        return locations;
    }

    /**
     * Writes
     * @param lengths of documents to docWeights.bin file
     */
    public void writeLengthOfDocument(List<Double> lengths){
        File file = new File(path,"docWeights.bin");
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(file))) {
            for(Double length:lengths) {
                outputStream.writeDouble(length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}