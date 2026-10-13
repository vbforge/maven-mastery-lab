package com.vbforge.reactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MultiModuleReactorDemoTest — verifies every concept shown in
 * {@link MultiModuleReactorDemo}.
 *
 * <p>These tests are always GREEN. Read them as an executable specification.
 */
@DisplayName("MultiModuleReactorDemo — API contracts")
class MultiModuleReactorDemoTest {

    @Nested
    @DisplayName("1. sample reactor")
    class ReactorTest {

        @Test
        @DisplayName("sampleReactor is the classic core → service → app chain")
        void shape() {
            List<Module> reactor = MultiModuleReactorDemo.sampleReactor();
            assertThat(reactor).extracting(Module::id)
                    .containsExactly("sample-core", "sample-service", "sample-app");
            assertThat(reactor.get(0).dependencies()).isEmpty();
            assertThat(reactor.get(1).dependencies()).containsExactly("sample-core");
            assertThat(reactor.get(2).dependencies()).containsExactly("sample-service");
        }
    }

    @Nested
    @DisplayName("2. Aggregator vs parent")
    class AggregatorVsParentTest {

        @Test
        @DisplayName("an aggregator-only pom has modules and no children")
        void aggregatorOnly() {
            PomSummary agg = MultiModuleReactorDemo.aggregatorOnlyPom();
            assertThat(MultiModuleReactorDemo.isAggregatorOnly(agg)).isTrue();
        }

        @Test
        @DisplayName("a parent-only pom is not an aggregator-only")
        void parentOnly() {
            PomSummary par = MultiModuleReactorDemo.parentOnlyPom();
            assertThat(MultiModuleReactorDemo.isAggregatorOnly(par)).isFalse();
        }

        @Test
        @DisplayName("aggregator + parent is not aggregator-only")
        void aggregatorAndParent() {
            PomSummary both = MultiModuleReactorDemo.aggregatorAndParentPom();
            assertThat(MultiModuleReactorDemo.isAggregatorOnly(both)).isFalse();
        }
    }

    @Nested
    @DisplayName("3. Build order")
    class BuildOrderTest {

        @Test
        @DisplayName("sample reactor builds core → service → app")
        void chain() {
            assertThat(MultiModuleReactorDemo.buildOrder(MultiModuleReactorDemo.sampleReactor()))
                    .containsExactly("sample-core", "sample-service", "sample-app");
        }

        @Test
        @DisplayName("declaration order is not the build order")
        void reorders() {
            List<Module> reactor = List.of(
                    Module.of("app", "service"),
                    Module.of("service", "core"),
                    Module.of("core"));
            assertThat(MultiModuleReactorDemo.buildOrder(reactor))
                    .containsExactly("core", "service", "app");
        }

        @Test
        @DisplayName("cycle → IllegalStateException")
        void cycle() {
            assertThatThrownBy(() -> MultiModuleReactorDemo.buildOrder(
                    MultiModuleReactorDemo.cyclicReactor()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("4. Selection flags")
    class SelectionFlagsTest {

        @Test
        @DisplayName("revision snippet uses ${revision} and <modules>")
        void revisionSnippet() {
            assertThat(MultiModuleReactorDemo.revisionSnippet())
                    .contains("<version>${revision}</version>")
                    .contains("<modules>")
                    .contains("<revision>");
        }
    }

    @Test
    @DisplayName("main() runs without exceptions and prints every section")
    void mainPrintsAllSections() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            MultiModuleReactorDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("=== the reactor ===")
                .contains("=== aggregator vs parent ===")
                .contains("=== build order ===")
                .contains("=== selection flags ===")
                .contains("=== ${revision} ===")
                .contains("=== cycles ===");
    }
}