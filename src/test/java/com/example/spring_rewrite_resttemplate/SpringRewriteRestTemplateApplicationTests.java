package com.example.spring_rewrite_resttemplate;

import com.example.spring_rewrite_resttemplate.util.RestTemplateToWebClientRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


class SpringRewriteRestTemplateApplicationTests implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RestTemplateToWebClientRecipe())
                .parser(JavaParser.fromJavaVersion()
                        .classpath("spring-web","spring-webflux", "reactor-core","spring-context")
                        .logCompilationWarningsAndErrors(true));
    }

    @Test
    public void refactoringRestTemplateToWebClient() {
        
        rewriteRun(
                // language=java
                java(
                        """
                                import org.springframework.web.client.RestTemplate;
                                
                                public class DemoService{
    
                                    private final RestTemplate restTemplate = new RestTemplate();
    
                                    public void getPostById(Long id) {
                                        restTemplate.getForObject("/posts/{id}", Void.class, id);
                                    }
                                }
                                """,
                        """ 
                                import org.springframework.web.reactive.function.client.WebClient;
                                
                                public class DemoService{
                                
                                    private final WebClient webClient = WebClient.create();
                                
                                    public void getPostById(Long id) {
                                        webClient.get().uri("/posts/{id}", id).retrieve().bodyToMono(Void.class).block();
                                    }
                                }
                                """
                )
        );
    }

}
