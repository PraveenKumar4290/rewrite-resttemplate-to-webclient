package com.example.spring_rewrite_resttemplate.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.*;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.Locale;
import java.util.Optional;

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
    private static final MethodMatcher webClientGetMatcher = new MethodMatcher("org.springframework.web.reactive.function.client.WebClient <constructor>(..)");
    private static final MethodMatcher webClientUriMatcher = new MethodMatcher("org.springframework.web.reactive.function.client.WebClient uri(..)");
    private static final MethodMatcher webClientRetrieveMatcher = new MethodMatcher("org.springframework.web.reactive.function.client.WebClient retrieve(..)");
    private static final MethodMatcher webClientBodyToMonoMatcher = new MethodMatcher("org.springframework.web.reactive.function.client.WebClient bodyToMono(..)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Preconditions.Check(new UsesType<>("org.springframework.web.client.RestTemplate", false), new JavaVisitor<>() {


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

            private J.VariableDeclarations.NamedVariable variableToRename;
            @Override
            public J visitVariableDeclarations(J.VariableDeclarations variableDeclarations, ExecutionContext executionContext) {
                J.VariableDeclarations vd = (J.VariableDeclarations) super.visitVariableDeclarations(variableDeclarations, executionContext);

                if (TypeUtils.isOfClassType(vd.getType(), "org.springframework.web.client.RestTemplate")) {
                    maybeRemoveImport("org.springframework.web.client.RestTemplate");
                    maybeAddImport("org.springframework.web.reactive.function.client.WebClient");

                    doAfterVisit(new ChangeFieldType(
                            vd.getTypeAsFullyQualified().withFullyQualifiedName("org.springframework.web.client.RestTemplate"),
                            vd.getTypeAsFullyQualified().withFullyQualifiedName("org.springframework.web.reactive.function.client.WebClient"))
                    );
                    for (J.VariableDeclarations.NamedVariable namedVariable : variableDeclarations.getVariables()) {
                        vd.getVariables().forEach(var -> {
                            if (var.getSimpleName().equals(namedVariable.getSimpleName())) {
                                String lowerCase = vd.getTypeAsFullyQualified()
                                        .withFullyQualifiedName("org.springframework.web.reactive.function.client.WebClient")
                                        .getClassName();
                                lowerCase = lowerCase.substring(0, 2).toLowerCase(Locale.ROOT) + lowerCase.substring(2);
                                doAfterVisit(new RenameVariable<>(var, lowerCase));
                            }
                        });
                    }
                }
                return vd;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                // Check if this is a RestTemplate.getForObject call
                System.out.println(method.getMethodType().getName());
                if (REST_TEMPLATE_WITH_GET_FOR_OBJECT.matches(method.getMethodType()) &&
                        TypeUtils.isOfClassType(method.getSelect().getType(), "org.springframework.web.client.RestTemplate")) {

                    // Extract the arguments
                    Expression url = method.getArguments().get(0);
                    Expression responseType = method.getArguments().get(1);
                    Expression uriVariables = method.getArguments().size() > 2 ? method.getArguments().get(2) : null;

                    JavaTemplate template = JavaTemplate.builder("get().uri(#{any(String)}, #{any()}).retrieve().bodyToMono(#{any()}).block()")
                            .javaParser(JavaParser.fromJavaVersion()
                                    .classpath("spring-web", "spring-webflux", "reactor-core", "spring-context"))
                            .build();

                    return template.apply(getCursor(), method.getCoordinates().replace(), new Object[]{url, uriVariables, responseType});

                }
                return super.visitMethodInvocation(method, ctx);
            }


        });

    }
}
