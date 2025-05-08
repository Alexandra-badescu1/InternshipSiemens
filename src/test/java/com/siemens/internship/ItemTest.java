package com.siemens.internship;

import com.siemens.internship.model.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTest {

    @Test
    void getId() {
        Item item = new Item();
        item.setId(1L);
        assertEquals(1L, item.getId());
    }

    @Test
    void setId() {
        Item item = new Item();
        item.setId(2L);
        assertEquals(2L, item.getId());
    }

    @Test
    void getName() {
        Item item = new Item();
        item.setName("Test Name");
        assertEquals("Test Name", item.getName());
    }

    @Test
    void setName() {
        Item item = new Item();
        item.setName("Another Name");
        assertEquals("Another Name", item.getName());
    }

    @Test
    void getDescription() {
        Item item = new Item();
        item.setDescription("This is a description.");
        assertEquals("This is a description.", item.getDescription());
    }

    @Test
    void setDescription() {
        Item item = new Item();
        item.setDescription("Description test");
        assertEquals("Description test", item.getDescription());
    }

    @Test
    void getStatus() {
        Item item = new Item();
        item.setStatus("Pending");
        assertEquals("Pending", item.getStatus());
    }

    @Test
    void setStatus() {
        Item item = new Item();
        item.setStatus("Completed");
        assertEquals("Completed", item.getStatus());
    }

    @Test
    void getEmail() {
        Item item = new Item();
        item.setEmail("example@example.com");
        assertEquals("example@example.com", item.getEmail());
    }

    @Test
    void setEmail() {
        Item item = new Item();
        item.setEmail("test@domain.com");
        assertEquals("test@domain.com", item.getEmail());
    }

}
// The above code is a JUnit test class for the Item entity.