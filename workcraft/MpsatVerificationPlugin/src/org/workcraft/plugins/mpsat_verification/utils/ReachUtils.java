package org.workcraft.plugins.mpsat_verification.utils;

import org.workcraft.plugins.mpsat_verification.MpsatVerificationSettings;
import org.workcraft.plugins.mpsat_verification.presets.VerificationMode;
import org.workcraft.plugins.mpsat_verification.presets.VerificationParameters;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.StgSettings;
import org.workcraft.plugins.stg.utils.MutexUtils;
import org.workcraft.types.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReachUtils {

    public static VerificationParameters getToolchainPreparationParameters() {
        return new VerificationParameters("Toolchain preparation of data",
                VerificationMode.UNDEFINED, 0,
                null, 0);
    }

    public static VerificationParameters getToolchainCompletionParameters() {
        return new VerificationParameters("Toolchain completion",
                VerificationMode.UNDEFINED, 0,
                null, 0);
    }

    public static VerificationParameters getDeadlockParameters() {
        return new VerificationParameters("Deadlock freeness",
                VerificationMode.DEADLOCK, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount());
    }

    private static final String CONSISTENCY_REACH =
            "// Checks whether the STG is consistent, i.e. rising and falling transitions of every signal alternate in all traces\n" +
            "exists s in SIGNALS \\ DUMMY {\n" +
            "    let Es = E s {\n" +
            "        $s & exists e in Es s.t. is_plus e { @e }\n" +
            "        |\n" +
            "        ~$s & exists e in Es s.t. is_minus e { @e }\n" +
            "    }\n" +
            "}\n";

    public static VerificationParameters getConsistencyParameters() {
        return new VerificationParameters("Consistency",
                VerificationMode.STG_REACHABILITY_CONSISTENCY, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount(),
                CONSISTENCY_REACH, true);
    }

    private static final String DUMMY_CHECK_REACH =
            "exists e in EVENTS {\n" +
            "    is_dummy e\n" +
            "}\n";

    private static final String OUTPUT_PERSISTENCY_SKIP_NAMES_REPLACEMENT =
            "/* insert signal names that should be skipped */"; // For example: "scanout__1", "scanout__2",

    private static final String OUTPUT_PERSISTENCY_EXCEPTION_PAIRS_REPLACEMENT =
            "/* insert signal pairs of output persistency exceptions */"; // For example: ("me_g1", "me_g2"), ("me_g2", "me_g1"),

    private static final String OUTPUT_PERSISTENCY_REACH =
            "// Checks whether the STG is output-persistent, i.e. no local signal can be disabled by any other signal,\n" +
            "// with the exception of the provided set of pairs of signals (e.g. mutex outputs).\n" +
            DUMMY_CHECK_REACH +
            "? fail \"Output persistency can currently be checked only for STGs without dummies\" :\n" +
            "let\n" +
            "    EXCEPTION_PAIRS = {" + OUTPUT_PERSISTENCY_EXCEPTION_PAIRS_REPLACEMENT + "(\"\", \"\")} \\ {(\"\", \"\")},\n" +
            "    SKIP_SIGNALS = S ( {" + OUTPUT_PERSISTENCY_SKIP_NAMES_REPLACEMENT + "\"\"} \\ {\"\"} ),\n" +
            "    EXCEPTION_SIGNALS = gather pair in EXCEPTION_PAIRS {\n" +
            "        (S pair[0], S pair[1])\n" +
            "    },\n" +
            "    TR = T EVENTS,\n" +
            "    TRL = T (LOCAL \\ SKIP_SIGNALS) * TR,\n" +
            "    TRPT = gather t in TRL s.t. ~is_minus t { t },\n" +
            "    TRMT = gather t in TRL s.t. ~is_plus t { t }\n" +
            "{\n" +
            "    exists t_loc in TRL {\n" +
            "        let\n" +
            "            pre_t_loc = pre t_loc,\n" +
            "            OTHER_LOC = (T S t_loc \\ {t_loc}) * (is_plus t_loc ? TRPT : is_minus t_loc ? TRMT : TR) {\n" +
            "            // Check if some t can disable t_loc without enabling any other transition labelled by S t_loc.\n" +
            "            exists t in post pre_t_loc * TR s.t. S t != S t_loc &\n" +
            "                    ~((S t, S t_loc) in EXCEPTION_SIGNALS) & ~is_empty((pre t \\ post t) * pre_t_loc) {\n" +
            "                forall t_loc1 in OTHER_LOC s.t. is_empty(pre t_loc1 * (pre t \\ post t)) {\n" +
            "                    exists p in pre t_loc1 \\ post t { ~$p }\n" +
            "                }\n" +
            "                &\n" +
            "                @t\n" +
            "            }\n" +
            "            &\n" +
            "            @t_loc\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    public static VerificationParameters getOutputPersistencyParameters() {
        return getOutputPersistencyParameters(Collections.emptyList());
    }

    public static VerificationParameters getOutputPersistencyParameters(
            Collection<Pair<String, String>> exceptionPairs) {

        return getOutputPersistencyParameters(exceptionPairs, Collections.emptyList());
    }

    public static VerificationParameters getOutputPersistencyParameters(
            Collection<Pair<String, String>> exceptionPairs, Collection<String> skipSignals) {

        String exceptionPairsString = exceptionPairs.stream()
                .map(pair -> "(\"" + pair.getFirst() + "\", \"" + pair.getSecond() + "\"), ")
                .collect(Collectors.joining());

        String skipSignalsString = skipSignals.stream()
                .map(signal -> "\"" + signal + "\", ")
                .collect(Collectors.joining());

        String reachOutputPersistence = OUTPUT_PERSISTENCY_REACH
                .replace(OUTPUT_PERSISTENCY_EXCEPTION_PAIRS_REPLACEMENT, exceptionPairsString)
                .replace(OUTPUT_PERSISTENCY_SKIP_NAMES_REPLACEMENT, skipSignalsString);

        return new VerificationParameters("Output persistency",
                VerificationMode.STG_REACHABILITY_OUTPUT_PERSISTENCY, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount(),
                reachOutputPersistence, true);
    }

    private static final String DI_INTERFACE_REACH =
            "// Checks whether the STG's interface is delay insensitive, i.e. an input transition cannot trigger another input transition\n" +
            DUMMY_CHECK_REACH +
            "? fail \"Delay insensitivity can currently be checked only for STGs without dummies\" :\n" +
            "let\n" +
            "    TRINP = T INPUTS * T EVENTS\n" +
            "{\n" +
            "    exists ti in TRINP {\n" +
            "        let pre_ti = pre ti {\n" +
            "            // Check if some ti_trig can trigger ti\n" +
            "            exists ti_trig in pre pre_ti * TRINP s.t. S ti_trig != S ti & ~is_empty((post ti_trig \\ pre ti_trig) * pre_ti) {\n" +
            "                forall p in pre_ti \\ post ti_trig { $p }\n" +
            "                &\n" +
            "                @ti_trig\n" +
            "            }\n" +
            "            &\n" +
            "            ~@S ti\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    public static VerificationParameters getDiInterfaceParameters() {
        return new VerificationParameters("Delay insensitive interface",
                VerificationMode.STG_REACHABILITY, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount(),
                DI_INTERFACE_REACH, true);
    }

    private static final String NO_LOCAL_SELF_TRIGGERING_REACH =
            "// Checks whether the STG is free of self-triggering output and internal signals\n" +
            DUMMY_CHECK_REACH +
            "? fail \"Absence of self-triggering can be checked only on STGs without dummies\" :\n" +
            "exists s in LOCAL {\n" +
            "    let s_tran=T s {\n" +
            "        exists t1 in s_tran, t2 in s_tran s.t.\n" +
            "            t1!=t2\n" +
            "            &\n" +
            "            ~ is_empty (pre t2 * (post t1 \\ pre t1))  // t1 structurally triggers t2\n" +
            "            &\n" +
            "            is_empty (pre t2 * (pre t1 \\ post t1))    // t2 is not disabled by t1\n" +
            "        {\n" +
            "            @t1 & ~@t2\n" +
            "            &\n" +
            "            forall p in pre t2 s.t. ~(p in post t1) {\n" +
            "                ~(p in pre t1) & $p\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    public static VerificationParameters getNoLocalSelfTriggeringParameters() {
        return new VerificationParameters("Absence of self-triggering local signals",
                VerificationMode.STG_REACHABILITY, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount(),
                NO_LOCAL_SELF_TRIGGERING_REACH, true);
    }

    private static final String INPUT_PROPERNESS_REACH =
            "// Checks whether the STG is input proper, i.e. no input can be triggered by an internal signal or disabled by a local signal.\n" +
            DUMMY_CHECK_REACH +
            "? fail \"Input properness can currently be checked only for STGs without dummies\" :\n" +
            "let\n" +
            "    TR = T EVENTS,\n" +
            "    TRINP = T INPUTS * TR,\n" +
            "    TRI = T INTERNAL * TR,\n" +
            "    TRL = T LOCAL * TR,\n" +
            "    TRPT = gather t in TRINP s.t. ~is_minus t { t },\n" +
            "    TRMT = gather t in TRINP s.t. ~is_plus t { t }\n" +
            "{\n" +
            "    exists t_inp in TRINP {\n" +
            "        let\n" +
            "            pre_t_inp = pre t_inp,\n" +
            "            OTHER_INP = (T S t_inp \\ {t_inp}) * (is_plus t_inp ? TRPT : is_minus t_inp ? TRMT : TR) {\n" +
            "            // Check if some t_int can trigger t_inp.\n" +
            "            exists t_int in pre pre_t_inp * TRI s.t. ~is_empty((post t_int \\ pre t_int) * pre_t_inp) {\n" +
            "                forall p in pre_t_inp \\ post t_int { $p }\n" +
            "                &\n" +
            "                @t_int\n" +
            "            }\n" +
            "            &\n" +
            "            ~@S t_inp\n" +
            "            |\n" +
            "            // Check if some t_loc can disable t_inp without enabling any other transition labelled by S t_inp.\n" +
            "            exists t_loc in post pre_t_inp * TRL s.t. ~is_empty((pre t_loc \\ post t_loc) * pre_t_inp) {\n" +
            "                forall t_inp1 in OTHER_INP s.t. is_empty(pre t_inp1 * (pre t_loc \\ post t_loc)) {\n" +
            "                    exists p in pre t_inp1 \\ post t_loc { ~$p }\n" +
            "                }\n" +
            "                &\n" +
            "                @t_loc\n" +
            "            }\n" +
            "            &\n" +
            "            @t_inp\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    public static VerificationParameters getInputPropernessParameters() {
        return new VerificationParameters("Input properness",
                VerificationMode.STG_REACHABILITY, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount(),
                INPUT_PROPERNESS_REACH, true);
    }

    private static final String SHADOW_TRANSITIONS_REPLACEMENT =
            "/* insert set of names of shadow transitions here */"; // For example: "x+/1", "x-", "y+", "y-/1"

    private static final String CONFORMATION_REACH =
            "// Check whether several STGs conform to each other.\n" +
            "// The enabled-via-dummies semantics is assumed for @.\n" +
            "// Configurations with maximal dummies are assumed to be allowed.\n" +
            "let\n" +
            "    // Set of phantom output transition names in the whole composed STG.\n" +
            "    SHADOW_OUTPUT_TRANSITIONS = T ( {" + SHADOW_TRANSITIONS_REPLACEMENT + "\"\"} \\ {\"\"} )\n" +
            "{\n" +
            "    // Optimisation: make sure phantom events are not in the configuration.\n" +
            "    forall e in E SHADOW_OUTPUT_TRANSITIONS \\ CUTOFFS { ~$e }\n" +
            "    &\n" +
            "    // Check if some output signal is enabled due to phantom transitions only;\n" +
            "    // this would mean that some component STG does not conform to the rest of the composition.\n" +
            "    exists o in OUTPUTS {\n" +
            "        let tran_o = T o {\n" +
            "            exists t in tran_o * SHADOW_OUTPUT_TRANSITIONS {\n" +
            "                forall p in pre t { $p }\n" +
            "            }\n" +
            "            &\n" +
            "            forall tt in tran_o \\ SHADOW_OUTPUT_TRANSITIONS { ~@tt }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    public static VerificationParameters getConformationParameters(Collection<String> shadowTransitionNames) {
        String reach = getConformationReach(shadowTransitionNames);
        return new VerificationParameters("Conformation",
                VerificationMode.STG_REACHABILITY_CONFORMATION, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount(),
                reach, true);
    }

    private static String getConformationReach(Collection<String> shadowTransitionNames) {
        String str = shadowTransitionNames.stream()
                .map(ref -> "\"" + ref + "\", ")
                .collect(Collectors.joining());

        return CONFORMATION_REACH.replace(SHADOW_TRANSITIONS_REPLACEMENT, str);
    }

    // REACH expression for checking if these two pairs of signals can be implemented by a mutex
    private static final String MUTEX_R1_REPLACEMENT = "/* insert r1 name here */";
    private static final String MUTEX_G1_REPLACEMENT = "/* insert g1 name here */";
    private static final String MUTEX_R2_REPLACEMENT = "/* insert r2 name here */";
    private static final String MUTEX_G2_REPLACEMENT = "/* insert g2 name here */";
    private static final String MUTEX_IMPLEMENTABILITY_STRICT_REACH =
            "// For given signals r1, r2, g1, g2, check whether g1/g2 can be implemented\n" +
            "// by a STRICT mutex with requests r1/r2 and grants g1/g2.\n" +
            "// The properties to check are:\n" +
            "//   r1&~g2 => nxt(g1)\n" +
            "//   ~r1 => ~nxt(g1)\n" +
            "//   r2&g2 => ~nxt(g1)\n" +
            "// (and the symmetric constraints for nxt(g2)).\n" +
            "// Furthermore, the mutual exclusion of the critical sections is checked:\n" +
            "// ~( (r1&g1) & (r2&g2) )\n" +
            "// Note that the latter property does not follow from the above constraints\n" +
            "// for the next state functions of the grants (e.g. in the initial state).\n" +
            "let\n" +
            "    r1s = S\"" + MUTEX_R1_REPLACEMENT + "\",\n" +
            "    g1s = S\"" + MUTEX_G1_REPLACEMENT + "\",\n" +
            "    r2s = S\"" + MUTEX_R2_REPLACEMENT + "\",\n" +
            "    g2s = S\"" + MUTEX_G2_REPLACEMENT + "\",\n" +
            "    r1 = $r1s,\n" +
            "    g1 = $g1s,\n" +
            "    r2 = $r2s,\n" +
            "    g2 = $g2s,\n" +
            "    g1nxt = 'g1s,\n" +
            "    g2nxt = 'g2s\n" +
            "{\n" +
            "    // constraints on nxt(g1)\n" +
            "    r1 & ~g2 & ~g1nxt  // negation of r1&~g2 => nxt(g1)\n" +
            "    |\n" +
            "    ~r1 & g1nxt        // negation of ~r1 => ~nxt(g1)\n" +
            "    |\n" +
            "    r2 & g2 & g1nxt    // negation of r2&g2 => ~nxt(g1)\n" +
            "    |\n" +
            "    // constraints on nxt(g2)\n" +
            "    r2 & ~g1 & ~g2nxt  // negation of r2&~g1 => nxt(g2)\n" +
            "    |\n" +
            "    ~r2 & g2nxt        // negation of ~r2 => ~nxt(g2)\n" +
            "    |\n" +
            "    r1 & g1 & g2nxt    // negation of r1&g1 => ~nxt(g2)\n" +
            "    |\n" +
            "    // mutual exclusion of critical sections\n" +
            "    r1 & g1 & r2 & g2\n" +
            "}\n";

    private static final String MUTEX_IMPLEMENTABILITY_RELAXED_REACH =
            "// For given signals r1, r2, g1, g2, check whether g1/g2 can be implemented\n" +
            "// by a RELAXED mutex with requests r1/r2 and grants g1/g2.\n" +
            "// The properties to check are:\n" +
            "//   nxt(g1) = r1 & (~r2 | ~g2)\n" +
            "//   nxt(g2) = r2 & (~r1 | ~g1)\n" +
            "// Furthermore, the mutual exclusion of the critical sections is checked:\n" +
            "// ~( (r1&g1) & (r2&g2) )\n" +
            "// Note that the latter property does not follow from the above constraints\n" +
            "// for the next state functions of the grants (e.g. in the initial state).\n" +
            "let\n" +
            "    r1s = S\"" + MUTEX_R1_REPLACEMENT + "\",\n" +
            "    g1s = S\"" + MUTEX_G1_REPLACEMENT + "\",\n" +
            "    r2s = S\"" + MUTEX_R2_REPLACEMENT + "\",\n" +
            "    g2s = S\"" + MUTEX_G2_REPLACEMENT + "\",\n" +
            "    r1 = $r1s,\n" +
            "    g1 = $g1s,\n" +
            "    r2 = $r2s,\n" +
            "    g2 = $g2s,\n" +
            "    g1nxt = 'g1s,\n" +
            "    g2nxt = 'g2s\n" +
            "{\n" +
            "    (g1nxt ^ (r1 & (~r2 | ~g2)))  // negated definition of nxt(g1)\n" +
            "    |\n" +
            "    (g2nxt ^ (r2 & (~r1 | ~g1)))  // negated definition of nxt(g2)\n" +
            "    |\n" +
            "    r1 & g1 & r2 & g2  // mutual exclusion of critical sections\n" +
            "}\n";

    public static List<VerificationParameters> getMutexImplementabilityParameters(Collection<Mutex> mutexes) {
        return mutexes.stream().map(ReachUtils::getMutexImplementabilityParameters).collect(Collectors.toList());
    }

    private static VerificationParameters getMutexImplementabilityParameters(Mutex mutex) {
        String reach = getMutexImplementabilityReach(mutex.getProtocol())
                .replace(MUTEX_R1_REPLACEMENT, mutex.r1.name)
                .replace(MUTEX_G1_REPLACEMENT, mutex.g1.name)
                .replace(MUTEX_R2_REPLACEMENT, mutex.r2.name)
                .replace(MUTEX_G2_REPLACEMENT, mutex.g2.name);

        String description = "Mutex implementability "
                + (mutex.getProtocol() == Mutex.Protocol.LATE ? "(late protocol) " : "(early protocol) ")
                + "for place " + MutexUtils.getMutexPlaceExtendedTitle(mutex);

        return new VerificationParameters(description,
                VerificationMode.STG_REACHABILITY, 0,
                MpsatVerificationSettings.getSolutionMode(),
                MpsatVerificationSettings.getSolutionCount(),
                reach, true) {

            @Override
            public String getDescriptiveSuffix() {
                return "-Mutex_implementability-" + mutex.name;
            }
        };
    }

    private static String getMutexImplementabilityReach(Mutex.Protocol mutexProtocol) {
        if (mutexProtocol == null) {
            mutexProtocol = StgSettings.getMutexProtocol();
        }
        return mutexProtocol == Mutex.Protocol.EARLY
                ? MUTEX_IMPLEMENTABILITY_RELAXED_REACH
                : MUTEX_IMPLEMENTABILITY_STRICT_REACH;
    }

    public static String getBooleanAsString(boolean value) {
        return value ? "true" : "false";
    }

}
