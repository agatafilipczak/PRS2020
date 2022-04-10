package prs.project;

import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import prs.project.checker.Ledger;
import prs.project.controllers.Settings;
import prs.project.model.Product;
import prs.project.model.Warehouse;
import prs.project.status.ReplyToAction;
import prs.project.task.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Service
@Slf4j
public class ParallelExecutor {

    @Autowired
    Ledger ledger;

    Settings settings;
    List<Akcja> akcje = new ArrayList<>();
    boolean active = true;
    Set<Enum> mojeTypy = new HashSet<>();
    ConcurrentLinkedDeque<Akcja> kolejka = new ConcurrentLinkedDeque();
    Warehouse magazyn = new Warehouse();
    EnumMap<Product, Long> sprzedaz = new EnumMap(Product.class);
    EnumMap<Product, Long> rezerwacje = new EnumMap(Product.class);
    Long promoLicznik = 0L;

    public ParallelExecutor(Settings settings, List<Akcja> akcje) {
        this.settings = settings;
        this.akcje = akcje;
        Arrays.stream(Product.values()).forEach(p -> sprzedaz.put(p, 0L));
        Arrays.stream(Product.values()).forEach(p -> rezerwacje.put(p, 0L));

        mojeTypy.addAll(Wycena.valueOf(settings.getWycena()).getAkceptowane());
        mojeTypy.addAll(Zamowienia.valueOf(settings.getZamowienia()).getAkceptowane());
        mojeTypy.addAll(Zaopatrzenie.valueOf(settings.getZaopatrzenie()).getAkceptowane());
        mojeTypy.addAll(Wydarzenia.valueOf(settings.getWydarzenia()).getAkceptowane());
        mojeTypy.addAll(Arrays.asList(SterowanieAkcja.values()));
        Thread thread = new Thread(() ->
        {
            while (active) {
                threadProcess();
            }
        });
        thread.start();
        Thread thread2 = new Thread(() ->
        {
            while (active) {
                threadProcess();
            }
        });
        thread2.start();
    }

    public void process(Akcja jednaAkcja) {
        Stream.of(jednaAkcja)
                .filter(akcja -> mojeTypy.contains(akcja.getTyp()))
                .forEach(akcja -> {
                    kolejka.add(akcja);
                });
    }

    public void threadProcess() {

        ReentrantLock lock_kolejka = new ReentrantLock();

        Akcja akcja = null;
        lock_kolejka.lock();
        if (!kolejka.isEmpty()) {
            try {
                akcja = kolejka.pollFirst();
            } finally {
                lock_kolejka.unlock();
            }
        }

        if (akcja != null) {
            ReplyToAction odpowiedz = procesujAkcje(akcja);
            try {
                wyslijOdpowiedzLokalnie(odpowiedz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ReplyToAction procesujAkcje(Akcja akcja) {

        /* tworzymy locki = blokady na te obiekty,
        jak mamy dwa rozne locki, na dwa rozne obiekty to wtedy 2 rone watki moga dzialac w tej metodzie
        jak zakladamy 2 locki w jednej metodzie, to w kazdym innych metodach musi byc ta sama kolejnosc!
        wtedy unikamy deadlocka
        * */
        ReentrantLock lock_magazyn = new ReentrantLock();
        ReentrantLock lock_sprzedaz = new ReentrantLock();


        log.info("Procesuje " + akcja.getTyp());
        ReplyToAction odpowiedz = ReplyToAction.builder()
                .typ(akcja.getTyp())
                .id(akcja.getId())
                .build();

        if (WycenaAkcje.PODAJ_CENE.equals(akcja.getTyp())) {

            lock_magazyn.lock();
            try {
                odpowiedz.setProdukt(akcja.getProduct());
                odpowiedz.setCena(magazyn.getCeny().get(akcja.getProduct()));
                if (mojeTypy.contains(Wycena.PROMO_CO_10_WYCEN)) {
                    promoLicznik++;
                    if (promoLicznik == 10)
                        odpowiedz.setCena(0L);
                }
            } finally {
                lock_magazyn.unlock();
            }
        }


        if (WydarzeniaAkcje.INWENTARYZACJA.equals(akcja.getTyp())) {
            lock_magazyn.lock();
            try {
                odpowiedz.setStanMagazynów(magazyn.getStanMagazynowy().clone());
            } finally {
                lock_magazyn.unlock();
            }
        }


        if (ZamowieniaAkcje.POJEDYNCZE_ZAMOWIENIE.equals(akcja.getTyp())) {


            lock_magazyn.lock();
            lock_sprzedaz.lock();
            try {
                odpowiedz.setProdukt(akcja.getProduct());
                odpowiedz.setLiczba(akcja.getLiczba());
                Long naMagazynie = magazyn.getStanMagazynowy().get(akcja.getProduct());
                if (naMagazynie >= akcja.getLiczba()) {
                    odpowiedz.setZrealizowaneZamowienie(true);
                    magazyn.getStanMagazynowy().put(akcja.getProduct(), naMagazynie - akcja.getLiczba());
                    sprzedaz.put(akcja.getProduct(), sprzedaz.get(akcja.getProduct()) + akcja.getLiczba());
                } else {
                    odpowiedz.setZrealizowaneZamowienie(false);
                }
            } finally {
                lock_magazyn.unlock();
            }
        }


        if (ZaopatrzenieAkcje.POJEDYNCZE_ZAOPATRZENIE.equals(akcja.getTyp())) {
            lock_magazyn.lock();
            try {
                odpowiedz.setProdukt(akcja.getProduct());
                odpowiedz.setLiczba(akcja.getLiczba());
                Long naMagazynie = magazyn.getStanMagazynowy().get(akcja.getProduct());
                odpowiedz.setZebraneZaopatrzenie(true);
                if (magazyn.getStanMagazynowy().get(akcja.getProduct()) >= 0) {
                    magazyn.getStanMagazynowy().put(akcja.getProduct(), naMagazynie + akcja.getLiczba());
                }
            } finally {
                lock_magazyn.unlock();
            }
        }


        if (SterowanieAkcja.ZAMKNIJ_SKLEP.equals(akcja.getTyp())) {
            odpowiedz.setStanMagazynów(magazyn.getStanMagazynowy());
            odpowiedz.setGrupaProduktów(magazyn.getCeny());
        }
        return odpowiedz;
    }

    public void wyslijOdpowiedz(ReplyToAction odpowiedz) throws IOException {
        odpowiedz.setStudentId(settings.getNumerIndeksu());
        HttpPost post = new HttpPost("http://localhost:8080/action/log");

        JsonMapper mapper = new JsonMapper();
        String json = mapper.writeValueAsString(odpowiedz);
        StringEntity entity = new StringEntity(json);
        log.info(json);
        post.setEntity(entity);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(post)) {

            HttpEntity rEntity = response.getEntity();
            if (rEntity != null) {
                // return it as a String
                String result = EntityUtils.toString(rEntity);
                log.info(result);
            }
        }
    }

    public void wyslijOdpowiedzLokalnie(ReplyToAction odpowiedz) throws IOException {
        odpowiedz.setStudentId(settings.getNumerIndeksu());
        try {
            ledger.addReply(odpowiedz);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(SterowanieAkcja.ZAMKNIJ_SKLEP.equals(odpowiedz.getTyp())) {
            Warehouse magazyn = new Warehouse();
            EnumMap<Product, Long> sprzedaz = new EnumMap(Product.class);
            EnumMap<Product, Long> rezerwacje = new EnumMap(Product.class);
            Arrays.stream(Product.values()).forEach(p -> rezerwacje.put(p, 0L));
            Arrays.stream(Product.values()).forEach(p -> sprzedaz.put(p, 0L));

            Long promoLicznik = 0L;
        }
    }
}
