package com.yichi.onepiece.character.model;

import com.yichi.onepiece.character.model.dao.CharacterDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:test-h2.properties")
public class CharacterDaoTests {
  @Autowired CharacterDao characterDao;
}
