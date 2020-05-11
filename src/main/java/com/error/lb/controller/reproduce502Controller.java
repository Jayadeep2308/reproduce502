package com.error.lb.controller;


import com.error.lb.model.sampleDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@CrossOrigin
public class reproduce502Controller {
    @PostMapping(value = {"/502"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public sampleDTO executePost(@Valid @RequestBody sampleDTO sampleInput) {
        sampleInput.setTotal(BigDecimal.ZERO);
        sampleInput.getItemList().forEach(itemId -> sampleInput.setTotal(sampleInput.getTotal().add(createRandomBigDecimal())));
        return sampleInput;
    }

    private BigDecimal createRandomBigDecimal() {
        var value = ThreadLocalRandom.current().nextInt();
        if (value > 0)
            return BigDecimal.valueOf(value);
        else
            return BigDecimal.valueOf(value).negate();
    }
}
