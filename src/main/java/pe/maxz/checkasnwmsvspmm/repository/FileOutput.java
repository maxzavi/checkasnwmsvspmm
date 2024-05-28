package pe.maxz.checkasnwmsvspmm.repository;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import pe.maxz.checkasnwmsvspmm.dto.Asn;
import pe.maxz.checkasnwmsvspmm.dto.Facility;

@Repository
public class FileOutput {
    @Value("${report.print}")
    private String print;
    public void writeAsn(String file, List<Asn> asns, LocalDate dt) throws IOException{
        if (!print.equalsIgnoreCase("T")) return;
        try (
            FileWriter fileWriter = new FileWriter("out/" + file + "_" + dt.toString().replaceAll("-", "") +".txt");
            PrintWriter printWriter = new PrintWriter(fileWriter);    
        ) {
            asns.forEach(t->{
                printWriter.println(t.facility_id__code() + ","+ t.shipment_nbr());
            });                
        } catch (Exception e) {
            throw e;
        }
    } 
    public void writeFacilities(String file, List<Facility> facilities) throws IOException{
        if (!print.equalsIgnoreCase("T")) return;
        try (
            FileWriter fileWriter = new FileWriter("out/" + file );
            PrintWriter printWriter = new PrintWriter(fileWriter);    
        ) {
            facilities.forEach(t->{
                printWriter.println(t.code());
            });                
        } catch (Exception e) {
            throw e;
        }
    } 
}
