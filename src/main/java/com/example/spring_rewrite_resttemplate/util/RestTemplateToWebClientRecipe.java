package com.example.spring_rewrite_resttemplate.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.*;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class RestTemplateToWebClientRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Refactoring rest-template to webclient";
    }

    @Override
    public String getDescription() {
        return "Refactoring rest-template to webclient.";
    }

    private static final MethodMatcher REST_TEMPLATE_WITH_NEW_CONSTRUCTOR = new MethodMatcher("org.springframework.web.client.RestTemplate <constructor>()");

    private static final MethodMatcher REST_TEMPLATE_WITH_GET_FOR_OBJECT = new MethodMatcher("org.springframework.web.client.RestTemplate getForObject(..)");
    private static final MethodMatcher webClientGetMatcher=new MethodMatcher("org.springframework.web.reactive.function.client.WebClient <constructor>(..)");
    private static final MethodMatcher webClientUriMatcher=new MethodMatcher("org.springframework.web.reactive.function.client.WebClient uri(..)");
    private static final MethodMatcher webClientRetrieveMatcher=new MethodMatcher("org.springframework.web.reactive.function.client.WebClient retrieve(..)");
    private static final MethodMatcher webClientBodyToMonoMatcher=new MethodMatcher("org.springframework.web.reactive.function.client.WebClient bodyToMono(..)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Preconditions.Check(new UsesType<>("org.springframework.web.client.RestTemplate", false), new JavaVisitor<>() {
            private J.VariableDeclarations.NamedVariable variableToRename;

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J visitedNewClass = super.visitNewClass(newClass, ctx);

                if (TypeUtils.isOfClassType(((J.NewClass) visitedNewClass).getType(), "org.springframework.web.client.RestTemplate")) {

                    if (REST_TEMPLATE_WITH_NEW_CONSTRUCTOR.matches(newClass)) {
                        maybeAddImport("org.springframework.web.reactive.function.client.WebClient");
                        maybeRemoveImport("org.springframework.web.client.RestTemplate");

                        JavaTemplate javaTemplate = JavaTemplate.builder("WebClient.create()")
                                .imports("org.springframework.web.reactive.function.client.WebClient")
                                .javaParser(JavaParser.fromJavaVersion()
                                        .classpath("spring-web", "spring-webflux", "reactor-core", "spring-context"))
                                .build();

                        return javaTemplate.apply(getCursor(), newClass.getCoordinates().replace());
                    }
                }
                return visitedNewClass;
            }

//            @Override
//            public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
//                J j = super.visitIdentifier(ident, ctx);
//
//                if (j instanceof J.Identifier && ((J.Identifier) j).getType() != null) {
//                    if (REST_TEMPLATE_WITH_NEW_CONSTRUCTOR.matches(ident)) {
//                        maybeAddImport("org.springframework.web.reactive.function.client.WebClient");
//                        maybeRemoveImport("org.springframework.web.client.RestTemplate");
//
//                        JavaTemplate javaTemplate = JavaTemplate.builder("WebClient")
//                                .imports("org.springframework.web.reactive.function.client.WebClient")
//                                .javaParser(JavaParser.fromJavaVersion()
//                                        .classpath("spring-web", "spring-webflux", "reactor-core", "spring-context"))
//                                .build();
//                       return javaTemplate.apply(getCursor(), ident.getCoordinates().replace());
//                    }
//                }
//                return j;
//            }

        });

    }
}
