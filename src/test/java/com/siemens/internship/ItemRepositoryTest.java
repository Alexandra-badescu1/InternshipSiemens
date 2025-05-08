package com.siemens.internship;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import(ValidatorConfig.class)  // Adăugați această linie pentru a importa configurația Validator
public class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private Validator validator;

    @Test
    public void testValidEmail() {
        Item item = new Item();
        item.setName("Item 1");
        item.setEmail("valid.email@example.com");

        Set<ConstraintViolation<Item>> violations = validator.validate(item);

        assertEquals(0, violations.size(), "There should be no violations for a valid email");

        itemRepository.save(item);

        List<Long> ids = itemRepository.findAllIds();
        assertEquals(1, ids.size(), "There should be 1 ID in the list after saving the item");
        assertTrue(ids.contains(item.getId()), "The list should contain the ID of the saved item");
    }

    @Test
    public void testInvalidEmail() {
        Item item = new Item();
        item.setName("Item 2");
        item.setEmail("invalid-email");

        Set<ConstraintViolation<Item>> violations = validator.validate(item);

        assertEquals(1, violations.size(), "There should be one violation for the invalid email");

        itemRepository.save(item);  // In a real-world scenario, handle this exception
    }
}
