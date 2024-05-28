package pe.maxz.checkasnwmsvspmm;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import lombok.extern.log4j.Log4j2;
import pe.maxz.checkasnwmsvspmm.repository.FileOutput;
import pe.maxz.checkasnwmsvspmm.repository.PmmRepository;
import pe.maxz.checkasnwmsvspmm.repository.WmsRepository;

@SpringBootApplication
@Log4j2
public class CheckasnwmsvspmmApplication implements CommandLineRunner{

	@Autowired
	private WmsRepository wmsRepository;

	@Autowired
	private PmmRepository pmmRepository;

	@Autowired
	private FileOutput fileOutput;
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(CheckasnwmsvspmmApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}
	@Override
	public void run(String... args) throws Exception {
		var dateFrom = getLocaldatefromString(args[0]);
		var dateTo =getLocaldatefromString(args[1]);
		log.info("Start from {} to {}", dateFrom, dateTo);
		//Check Facilities by user
		var facilitiesDiff = wmsRepository.checkFaccilitiesByUser();
		log.info("Diff facilities: qty {}", facilitiesDiff.size());

		fileOutput.writeFacilities("facilitiesDiff.txt", facilitiesDiff);
		dateFrom.datesUntil(dateTo.plusDays(1)).forEach(dt->{

			try {
				var asns = wmsRepository.getAsns(dt);
				fileOutput.writeAsn("dataasnfull", asns, dt);
				var diffs = pmmRepository.getDiffs(asns);
				fileOutput.writeAsn("dataasndiff", diffs, dt);
				log.info("Date: {}, Asns: {}, diffs {}", dt, asns.size(), diffs.size());					
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)){
					log.info("Date: {}, Asns: {}",dt, 0);					
				}else{
					throw e;
				}
			}catch(Exception e){
				log.error(e);
			}
		});
		log.info("End");
	}
	private LocalDate getLocaldatefromString(String yyyymmdd){
		int year  = Integer.parseInt(yyyymmdd.substring(0, 4));
		int month = Integer.parseInt(yyyymmdd.substring(4, 6));
		int day = Integer.parseInt(yyyymmdd.substring(6, 8));
		log.debug("year {} month {} day {}", year,month,day);		
		return LocalDate.of(year,month,day);
	}

}
