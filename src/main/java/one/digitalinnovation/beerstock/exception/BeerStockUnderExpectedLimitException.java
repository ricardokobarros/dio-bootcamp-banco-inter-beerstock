package one.digitalinnovation.beerstock.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BeerStockUnderExpectedLimitException extends Exception {

    public BeerStockUnderExpectedLimitException(Long id, int quantityToDecrement) {
        super(String.format("Beers with %s ID to decrement informed is bellow the min stock expected: %s", id, quantityToDecrement));
    }
}
