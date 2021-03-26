package one.digitalinnovation.beerstock.service

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder
import one.digitalinnovation.beerstock.dto.BeerDTO
import one.digitalinnovation.beerstock.entity.Beer
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException
import one.digitalinnovation.beerstock.exception.BeerNotFoundException
import one.digitalinnovation.beerstock.exception.BeerStockExceededException
import one.digitalinnovation.beerstock.exception.BeerStockUnderExpectedLimitException
import one.digitalinnovation.beerstock.mapper.BeerMapper
import one.digitalinnovation.beerstock.repository.BeerRepository
import one.digitalinnovation.beerstock.service.BeerService
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.FailsWith
import spock.lang.Specification

@SpringBootTest
class BeerServiceTestSpec extends Specification {


    private static final long INVALID_BEER_ID = 1L;

    @SpringBean
    BeerRepository beerRepository = Mock();

    private BeerMapper beerMapper = BeerMapper.INSTANCE;


    private BeerService beerService;

    def setup(){
        beerService = new BeerService(beerRepository);
    }

    def whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedSavedBeer = beerMapper.toModel(expectedBeerDTO);

        and:

        1 * beerRepository.findByName(expectedBeerDTO.getName()) >> Optional.empty();
        1 * beerRepository.save(expectedSavedBeer) >> expectedSavedBeer;

        when:
        BeerDTO createdBeerDTO = beerService.createBeer(expectedBeerDTO);

        then:
        createdBeerDTO.getId() == expectedBeerDTO.getId();
        createdBeerDTO.getName() == expectedBeerDTO.getName();
        createdBeerDTO.getQuantity() == expectedBeerDTO.getQuantity();
    }

    @FailsWith(BeerAlreadyRegisteredException)
    def whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer duplicatedBeer = beerMapper.toModel(expectedBeerDTO);

        and:
        1 * beerRepository.findByName(expectedBeerDTO.getName()) >> Optional.of(duplicatedBeer);

        expect:
        beerService.createBeer(expectedBeerDTO)

    }

    def whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        given:
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);

        and:
        1 * beerRepository.findByName(expectedFoundBeer.getName()) >> Optional.of(expectedFoundBeer);

        when:
        BeerDTO foundBeerDTO = beerService.findByName(expectedFoundBeerDTO.getName());

        then:
        foundBeerDTO == expectedFoundBeerDTO;
    }

    @FailsWith(BeerNotFoundException)
    def whenNotRegisteredBeerNameIsGivenThenThrowAnException() {
        given:
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        and:
        1 * beerRepository.findByName(expectedFoundBeerDTO.getName()) >> Optional.empty();

        expect:
        beerService.findByName(expectedFoundBeerDTO.getName());
    }

    def whenListBeerIsCalledThenReturnAListOfBeers() {
        given:
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);

        and:
        1 * beerRepository.findAll() >> Collections.singletonList(expectedFoundBeer);

        when:
        List<BeerDTO> foundListBeersDTO = beerService.listAll();

        then:
        !foundListBeersDTO.isEmpty()
        foundListBeersDTO.get(0) == expectedFoundBeerDTO

    }

    def whenListBeerIsCalledThenReturnAnEmptyListOfBeers() {
        when:
        beerRepository.findAll() >> Collections.EMPTY_LIST;

        then:
        List<BeerDTO> foundListBeersDTO = beerService.listAll();
        foundListBeersDTO.isEmpty()
    }


    def whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException{
        given:
        BeerDTO expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedDeletedBeer = beerMapper.toModel(expectedDeletedBeerDTO);

        when:
        beerService.deleteById(expectedDeletedBeerDTO.getId());

        then:
        1 * beerRepository.findById(expectedDeletedBeerDTO.getId()) >>  Optional.of(expectedDeletedBeer);
        1 * beerRepository.deleteById(expectedDeletedBeerDTO.getId()) >> {};
    }



    def whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        and:
        beerRepository.findById(expectedBeerDTO.getId()) >> Optional.of(expectedBeer);
        beerRepository.save(expectedBeer) >> expectedBeer;
        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = expectedBeerDTO.getQuantity() + quantityToIncrement;

        when:
        BeerDTO incrementedBeerDTO = beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);

        then:
        expectedQuantityAfterIncrement == incrementedBeerDTO.getQuantity();
        expectedQuantityAfterIncrement < expectedBeerDTO.getMax();
    }

    @FailsWith(BeerStockExceededException)
    def whenIncrementIsGreatherThanMaxThenThrowException() {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        and:
        beerRepository.findById(expectedBeerDTO.getId()) >> Optional.of(expectedBeer);
        int quantityToIncrement = 80;

        expect:
        beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);
    }

    @FailsWith(BeerStockExceededException)
    def whenIncrementAfterSumIsGreatherThanMaxThenThrowException() {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        and:
        1 * beerRepository.findById(expectedBeerDTO.getId()) >> Optional.of(expectedBeer);
        int quantityToIncrement = 45;

        expect:
        beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);
    }

    @FailsWith(BeerNotFoundException)
    def whenIncrementIsCalledWithInvalidIdThenThrowException() {
        given:
        int quantityToIncrement = 10;

        and:
        1 * beerRepository.findById(INVALID_BEER_ID) >> Optional.empty();

        expect:
        beerService.increment(INVALID_BEER_ID, quantityToIncrement);
    }

    def whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        and:
        1 * beerRepository.findById(expectedBeerDTO.getId()) >> Optional.of(expectedBeer);
        1 * beerRepository.save(expectedBeer) >> expectedBeer;
        int quantityToDecrement = 5;
        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityToDecrement;

        when:
        BeerDTO incrementedBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement);

        then:
        expectedQuantityAfterDecrement == incrementedBeerDTO.getQuantity();
        expectedQuantityAfterDecrement > 0;
    }

    def whenDecrementIsCalledToEmptyStockThenEmptyBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        and:
        1 * beerRepository.findById(expectedBeerDTO.getId()) >> Optional.of(expectedBeer);
        1 * beerRepository.save(expectedBeer) >> expectedBeer;
        int quantityToDecrement = 10;
        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityToDecrement;

        when:
        BeerDTO incrementedBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement);

        then:
        expectedQuantityAfterDecrement == 0;
        expectedQuantityAfterDecrement == incrementedBeerDTO.getQuantity();
    }

    @FailsWith(BeerStockUnderExpectedLimitException)
    def whenDecrementIsLowerThanZeroThenThrowException() {
        given:
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        and:
        1 * beerRepository.findById(expectedBeerDTO.getId()) >> Optional.of(expectedBeer);
        int quantityToDecrement = 80;

        expect:
        beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement);
    }

    @FailsWith(BeerNotFoundException)
    def whenDecrementIsCalledWithInvalidIdThenThrowException() {
        given:
        int quantityToDecrement = 10;

        and:
        1 * beerRepository.findById(INVALID_BEER_ID) >> Optional.empty();

        expect:
        beerService.decrement(INVALID_BEER_ID, quantityToDecrement);
    }

}
