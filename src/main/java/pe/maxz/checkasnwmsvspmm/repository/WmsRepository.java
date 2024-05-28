package pe.maxz.checkasnwmsvspmm.repository;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import lombok.extern.log4j.Log4j2;
import pe.maxz.checkasnwmsvspmm.dto.Asn;
import pe.maxz.checkasnwmsvspmm.dto.Facility;

@Repository
@Log4j2
public class WmsRepository {
    @Value("${wms.api.uribase}")
    private String uriBase;

    @Value("${wms.api.username}")
    private String username;

    @Value("${wms.api.password}")
    private String password;

    @Value("${wms.api.pagesize}")
    private String pageSize;


    @SuppressWarnings("null")
    public List<Asn> getAsns(LocalDate date){
        int page = 1;
        boolean next=true;
        record Result(String next_page, List<Asn> results) {}

        var result = new ArrayList<Asn>();
        while (next) {
            RestClient restClient = RestClient.create();        
            var dataIni = date+"T00:00:00";
            var dataEnd = date+"T23:59:59";
            log.debug("Range from {} to {}", dataIni, dataEnd);
            
            var response = restClient.get()
                .uri(uriBase  
                    + "/entity/ib_shipment" 
                    + "?verified_ts__range={0},{1}&values_list={2}&page_size={3}&page={4}",
                    dataIni,
                    dataEnd,
                    "shipment_nbr,facility_id__code",
                    pageSize, 
                    page)
                .header("Authorization", getAuthHeader(username, password))
                .retrieve()
                .body(Result.class);
                //var response = getAsns(date, page);
            if (response.next_page()==null) next=false;
            page++;
            result.addAll(response.results());           
        }
        return result;
    }

    public List<Facility> checkFaccilitiesByUser(){
        int userId = getUserId();
        log.info("User {} -> Id: {}", username, userId);
        var facilities = getAllFacilities();
        log.info("Facilities Qty: {}", facilities.size());
        var faccilitiesByUser = getFacilitiesByUser(userId);
        log.info("Facilities by User: {}", faccilitiesByUser.size());
        //Compare
        var facDiff = new ArrayList<Facility>();
        facDiff.addAll(facilities);
        facDiff.removeAll(faccilitiesByUser);
        log.debug("Diff: qty {}, {}", facDiff.size(), facDiff);
        return facDiff;
    }

    @SuppressWarnings("null")
    private int getUserId(){
        RestClient restClient = RestClient.create();

        record User(int id){};
        record Result(List<User> results){};

        var response = restClient.get()
            .uri(uriBase  
                + "/entity/user" 
                + "?auth_user_id__username={0}&values_list={1}",
                username,
                "id")
            .header("Authorization", getAuthHeader(username, password))
            .retrieve()
            .body(Result.class);

        return response.results.get(0).id;
    }

    @SuppressWarnings("null")
    private List<Facility> getAllFacilities(){
        RestClient restClient = RestClient.create();
        record Result(String next_page, List<Facility> results){};

        List<Facility> facilities = new ArrayList<Facility>();

        boolean next=true;
        int page =1 ;
        while(next){
            var response = restClient.get()
            .uri(uriBase  
                + "/entity/facility" 
                + "?values_list={0}&page_size={1}&page={2}&wms_managed_flg={3}",
                "code,id",
                pageSize,
                page,
                true)
            .header("Authorization", getAuthHeader(username, password))
            .retrieve()
            .body(Result.class);
            facilities.addAll(response.results);
            page++;
            if(response.next_page==null) next=false;
        }

        return facilities;
    }

    @SuppressWarnings("null")
    private List<Facility> getFacilitiesByUser(int userId){
        RestClient restClient = RestClient.create();
        record FacilityId (int id, String key){};
        record Detail (FacilityId facility_id){};
        record Result(String next_page, List<Detail> results){};
        
        List<Facility> facilities = new ArrayList<Facility>();

        boolean next=true;
        int page =1 ;
        while(next){
            var response = restClient.get()
            .uri(uriBase  
                + "/entity/user_facility" 
                + "?cw_user_id={0}&page_size={1}&page={2}",
                userId,
                pageSize,
                page)
            .header("Authorization", getAuthHeader(username, password))
            .retrieve()
            .body(Result.class);
            response.results.forEach(t->{
                facilities.add(new Facility(t.facility_id.id, t.facility_id.key));
            });
            page++;
            if(response.next_page==null) next=false;
        }
        return facilities;
    }

    private String getAuthHeader(String username, String password){
        //Auth
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode( auth.getBytes(Charset.forName("US-ASCII")));
        return "Basic " + new String( encodedAuth );
    }
}
