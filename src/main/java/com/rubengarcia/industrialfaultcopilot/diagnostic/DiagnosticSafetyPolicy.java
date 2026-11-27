package com.rubengarcia.industrialfaultcopilot.diagnostic;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class DiagnosticSafetyPolicy {

    private static final List<Pattern> HIGH_RISK_PATTERNS = List.of(
            Pattern.compile("\\b(electric|electrical|electricity|voltage|current|energized|live wire|shock|electrico|electrica|electricidad|tension|corriente|energizado|descarga)\\b"),
            Pattern.compile("\\b(spark|sparks|smoke|fire|flame|burn|burning|overheat|smell of burning|chispa|chispas|humo|fuego|llama|quemado|quemando|sobrecalentamiento)\\b"),
            Pattern.compile("\\b(pressure|pressurized|pneumatic|hydraulic|compressed air|presion|presurizado|neumatico|hidraulico|aire comprimido)\\b"),
            Pattern.compile("\\b(interlock|safety guard|safety door|guard switch|puerta de seguridad|proteccion de seguridad|enclavamiento|interbloqueo)\\b"),
            Pattern.compile("\\b(moving part|moving parts|rotating|mechanical failure|crushed|pinched|parte movil|partes moviles|girando|atrapamiento|aplastamiento|falla mecanica)\\b"),
            Pattern.compile("\\b(high temperature|hot surface|heated|thermal|temperatura alta|superficie caliente|calentamiento|termico|termica)\\b")
    );

    public boolean requiresEscalation(DiagnosticRequest request) {
        if (request == null) {
            return true;
        }

        String content = normalize(String.join(" ",
                safe(request.machine()),
                safe(request.symptom()),
                safe(request.alarmCode()),
                safe(request.observations())
        ));

        return HIGH_RISK_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(content).find());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
