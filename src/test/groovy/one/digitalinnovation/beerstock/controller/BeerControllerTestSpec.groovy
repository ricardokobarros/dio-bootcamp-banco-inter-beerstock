package one.digitalinnovation.beerstock.controller

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder
import one.digitalinnovation.beerstock.dto.BeerDTO
import one.digitalinnovation.beerstock.dto.QuantityDTO
import one.digitalinnovation.beerstock.enums.BeerType
import one.digitalinnovation.beerstock.exception.BeerNotFoundException
import one.digitalinnovation.beerstock.exception.BeerStockExceededException
import one.digitalinnovation.beerstock.exception.BeerStockUnderExpectedLimitException
import one.digitalinnovation.beerstock.service.BeerService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static one.digitalinnovation.beerstock.utils.JsonConvertionUtils.asJsonString
import static org.hamcrest.core.Is.is
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

//    https://blog.allegro.tech/2018/04/Spring-WebMvcTest-with-Spock.html
//https://objectpartners.com/2018/06/14/spock-1-2-annotations-for-spring-integration-testing/
@AutoConfigureMockMvc
@SpringBootTest
class BeerControllerTestSpec extends Specification {

    private static final String BEER_API_URL_PATH = "/api/v1/beers";
    private static final long VALID_BEER_ID = 1L;
    private static final long INVALID_BEER_ID = 2l;
    private static final String BEER_API_SUBPATH_INCREMENT_URL = "/increment";
    private static final String BEER_API_SUBPATH_DECREMENT_URL = "/decrement";

    @Autowired
    MockMvc mockMvc;

    @SpringBean
    BeerService beerService = Mock();

    def whenPOSTIsCalledThenABeerIsCreated() throws Exception {
        given:
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        and:
        1 * beerService.createBeer(beerDTO) >> beerDTO;

        when:
        def results = mockMvc.perform(post(BEER_API_URL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(beerDTO)))

        then:
        results.andExpect(status().isCreated())
                .andExpect(jsonPath('$.name', is(beerDTO.getName())))
                .andExpect(jsonPath('$.brand', is(beerDTO.getBrand())))
                .andExpect(jsonPath('$.type', is(beerDTO.getType().toString())));

    }

    def whenPOSTIsCalledWithoutRequiredFieldThenAnErrorIsReturned() throws Exception {
        given:
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        when:
        beerDTO.setName(name);
        beerDTO.setBrand(brand);
        beerDTO.setMax(max);
        beerDTO.setQuantity(quantity);
        beerDTO.setType(type);
        def results = mockMvc.perform(post(BEER_API_URL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(beerDTO)))

        then:
        results.andExpect(expectedResult);

        where:
        name | brand    | max | quantity | type           || expectedResult
        null | 'brand' | 0   | 0        | BeerType.LAGER || status().isBadRequest();
        'name' | null | 0 | 0 | BeerType.LAGER || status().isBadRequest();
        'name' | 'brand' | null | 0 | BeerType.LAGER || status().isBadRequest();
        'name' | 'brand' | 0 | null | BeerType.LAGER || status().isBadRequest();
        'name' | 'brand' | 0 | 0 | null || status().isBadRequest();


    }


    def whenGETIsCalledWithValidNameThenOkStatusIsReturned() throws Exception {
        given:
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        and:
        1 * beerService.findByName(beerDTO.getName()) >> beerDTO;

        when:
        def results = mockMvc.perform(get(BEER_API_URL_PATH + "/" + beerDTO.getName())
                .contentType(MediaType.APPLICATION_JSON))
        then:
        results.andExpect(status().isOk())
                .andExpect(jsonPath('$.name', is(beerDTO.getName())))
                .andExpect(jsonPath('$.brand', is(beerDTO.getBrand())))
                .andExpect(jsonPath('$.type', is(beerDTO.getType().toString())));
    }


    def whenGETIsCalledWithoutRegisteredNameThenNotFoundStatusIsReturned() throws Exception {
        given:
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        and:
        1 * beerService.findByName(beerDTO.getName()) >> { throw new BeerNotFoundException(beerDTO.getName()) }

        when:
        def results = mockMvc.perform(get(BEER_API_URL_PATH + "/" + beerDTO.getName())
                .contentType(MediaType.APPLICATION_JSON))

        then:
        results.andExpect(status().isNotFound());
    }


    def whenGETListWithBeersIsCalledThenOkStatusIsReturned() throws Exception {
        given:
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        and:
        1 * beerService.listAll() >> Collections.singletonList(beerDTO);

        when:
        def results = mockMvc.perform(get(BEER_API_URL_PATH)
                .contentType(MediaType.APPLICATION_JSON))

        then:
        results.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].name', is(beerDTO.getName())))
                .andExpect(jsonPath('$[0].brand', is(beerDTO.getBrand())))
                .andExpect(jsonPath('$[0].type', is(beerDTO.getType().toString())));
    }


    def whenGETListWithoutBeersIsCalledThenOkStatusIsReturned() throws Exception {
        given:
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        and:
        1 * beerService.listAll() >> Collections.singletonList(beerDTO);

        when:
        def results = mockMvc.perform(get(BEER_API_URL_PATH)
                .contentType(MediaType.APPLICATION_JSON))

        then:
        results.andExpect(status().isOk());
    }


    def whenDELETEIsCalledWithValidIdThenNoContentStatusIsReturned() throws Exception {
        given:
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        and:
        1 * beerService.deleteById(beerDTO.getId()) >> {};

        when:
        def results = mockMvc.perform(delete(BEER_API_URL_PATH + "/" + beerDTO.getId())
                .contentType(MediaType.APPLICATION_JSON))

        then:
        results.andExpect(status().isNoContent());
    }


    def whenDELETEIsCalledWithInvalidIdThenNotFoundStatusIsReturned() throws Exception {
        given:
        1 * beerService.deleteById(INVALID_BEER_ID) >> { throw new BeerNotFoundException(INVALID_BEER_ID) };
        when:
        def results = mockMvc.perform(delete(BEER_API_URL_PATH + "/" + INVALID_BEER_ID)
                .contentType(MediaType.APPLICATION_JSON))

        then:
        results.andExpect(status().isNotFound());
    }


    def whenPATCHIsCalledToIncrementDiscountThenOKstatusIsReturned() throws Exception {
        given:
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(10)
                .build();

        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        beerDTO.setQuantity(beerDTO.getQuantity() + quantityDTO.getQuantity());

        and:
        1 * beerService.increment(VALID_BEER_ID, quantityDTO.getQuantity()) >> beerDTO;

        when:
        def results = mockMvc.perform(patch(BEER_API_URL_PATH + "/" + VALID_BEER_ID + BEER_API_SUBPATH_INCREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO))).andExpect(status().isOk())

        then:
        results.andExpect(jsonPath('$.name', is(beerDTO.getName())))
                .andExpect(jsonPath('$.brand', is(beerDTO.getBrand())))
                .andExpect(jsonPath('$.type', is(beerDTO.getType().toString())))
                .andExpect(jsonPath('$.quantity', is(beerDTO.getQuantity())));
    }


    def whenPATCHIsCalledToIncrementGreatherThanMaxThenBadRequestStatusIsReturned() throws Exception {
        given:
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(30)
                .build();
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        beerDTO.setQuantity(beerDTO.getQuantity() + quantityDTO.getQuantity());

        and:
        1 * beerService.increment(VALID_BEER_ID, quantityDTO.getQuantity()) >> { throw new BeerStockExceededException(beerDTO.getId(), quantityDTO.quantity) };

        when:
        def results = mockMvc.perform(patch(BEER_API_URL_PATH + "/" + VALID_BEER_ID + BEER_API_SUBPATH_INCREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO)))
        then:
        results.andExpect(status().isBadRequest());
    }

    def whenPATCHIsCalledWithInvalidBeerIdToIncrementThenNotFoundStatusIsReturned() throws Exception {
        given:
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(30)
                .build();

        and:
        1 * beerService.increment(INVALID_BEER_ID, quantityDTO.getQuantity()) >> { throw new BeerNotFoundException(INVALID_BEER_ID) };

        when:
        def results = mockMvc.perform(patch(BEER_API_URL_PATH + "/" + INVALID_BEER_ID + BEER_API_SUBPATH_INCREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO)))

        then:
        results.andExpect(status().isNotFound());
    }

    void whenPATCHIsCalledToDecrementDiscountThenOKstatusIsReturned() throws Exception {
        given:
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(5)
                .build();

        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        beerDTO.setQuantity(beerDTO.getQuantity() - quantityDTO.getQuantity());

        and:
        1 * beerService.decrement(VALID_BEER_ID, quantityDTO.getQuantity()) >> beerDTO;

        when:
        def results = mockMvc.perform(patch(BEER_API_URL_PATH + "/" + VALID_BEER_ID + BEER_API_SUBPATH_DECREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO))).andExpect(status().isOk())
        then:
        results.andExpect(jsonPath('$.name', is(beerDTO.getName())))
                .andExpect(jsonPath('$.brand', is(beerDTO.getBrand())))
                .andExpect(jsonPath('$.type', is(beerDTO.getType().toString())))
                .andExpect(jsonPath('$.quantity', is(beerDTO.getQuantity())));
    }


    void whenPATCHIsCalledToDecrementLowerThanZeroThenBadRequestStatusIsReturned() throws Exception {
        given:
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(60)
                .build();

        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        beerDTO.setQuantity(beerDTO.getQuantity() - quantityDTO.getQuantity());

        and:
        1 * beerService.decrement(VALID_BEER_ID, quantityDTO.getQuantity()) >> { throw new BeerStockUnderExpectedLimitException(VALID_BEER_ID, quantityDTO.getQuantity()) };

        when:
        def results = mockMvc.perform(patch(BEER_API_URL_PATH + "/" + VALID_BEER_ID + BEER_API_SUBPATH_DECREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO)))
        then:
        results.andExpect(status().isBadRequest());
    }


    void whenPATCHIsCalledWithInvalidBeerIdToDecrementThenNotFoundStatusIsReturned() throws Exception {
        given:
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(5)
                .build();

        and:
        1 * beerService.decrement(INVALID_BEER_ID, quantityDTO.getQuantity()) >> { throw new BeerNotFoundException(INVALID_BEER_ID) };

        when:
        def results = mockMvc.perform(patch(BEER_API_URL_PATH + "/" + INVALID_BEER_ID + BEER_API_SUBPATH_DECREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO)))
        then:
        results.andExpect(status().isNotFound());
    }


}
