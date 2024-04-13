package at.aau.serg.websocketserver.modelmapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    // https://modelmapper.org/getting-started/
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // configuration for ModelMapper: match nested objects and allow us to convert from one to the other.
        // --> when we send a nested object in the DTO, it will be converted to a nested Entity object.
        // That is all Spring Data JPA needs to do the rest.
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return modelMapper;
    }
    // now we have access to the modelmapper inside our application context
}
