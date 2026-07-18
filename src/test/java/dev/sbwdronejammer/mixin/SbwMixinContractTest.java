package dev.sbwdronejammer.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SbwMixinContractTest {
    private static final String EXPECTED_SHA256 =
            "6eece0df9c927bddee3bd373fe90a2bbcba7157e9408ee0f94d13e04dc638395";
    private static final String DRONE =
            "com/atsuishio/superbwarfare/entity/vehicle/DroneEntity";
    private static final String GEO_VEHICLE =
            "com/atsuishio/superbwarfare/entity/vehicle/base/GeoVehicleEntity";
    private static final String VEHICLE =
            "com/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity";
    private static final Map<String, String> VEHICLE_CONTROLS = Map.ofEntries(
            Map.entry("setPower(F)V", "setPower"),
            Map.entry("getPower()F", "getPower"),
            Map.entry("setLeftInputDown(Z)V", "setLeftInputDown"),
            Map.entry("setRightInputDown(Z)V", "setRightInputDown"),
            Map.entry("setForwardInputDown(Z)V", "setForwardInputDown"),
            Map.entry("setBackInputDown(Z)V", "setBackInputDown"),
            Map.entry("setUpInputDown(Z)V", "setUpInputDown"),
            Map.entry("setDownInputDown(Z)V", "setDownInputDown")
    );

    @Test
    void mixinMembersMatchTheExactProductionJarAndTheirDeclaringClasses() throws Exception {
        Path productionJar = Path.of(System.getProperty("sbw.production.jar"));
        assertTrue(Files.isRegularFile(productionJar), "Missing production SBW JAR: " + productionJar);
        assertEquals(EXPECTED_SHA256, sha256(productionJar), "Unexpected SuperbWarfare production JAR");

        ClassContract drone = readJarClass(productionJar, DRONE);
        ClassContract geoVehicle = readJarClass(productionJar, GEO_VEHICLE);
        ClassContract vehicle = readJarClass(productionJar, VEHICLE);

        assertEquals(GEO_VEHICLE, drone.superName);
        assertEquals(VEHICLE, geoVehicle.superName);
        assertTrue(drone.methods.contains("travel()V"));
        String controllerShadow = "CONTROLLER:Lnet/minecraft/network/syncher/EntityDataAccessor;";
        assertTrue(drone.fields.contains(controllerShadow));
        assertTrue((drone.fieldAccess.get(controllerShadow) & Opcodes.ACC_FINAL) != 0,
                "The production CONTROLLER field must be final");

        for (String member : VEHICLE_CONTROLS.keySet()) {
            assertTrue(vehicle.methods.contains(member), member + " must be declared by VehicleEntity");
            assertFalse(geoVehicle.methods.contains(member), member + " must not be declared by GeoVehicleEntity");
            assertFalse(drone.methods.contains(member), member + " must not be shadowed as a DroneEntity member");
        }

        MixinContract access = readMixinClass(VehicleControlAccess.class);
        assertEquals(List.of(VEHICLE.replace('/', '.')), access.targets);
        assertEquals(VEHICLE_CONTROLS, access.invokersByTargetSignature);

        MixinContract droneMixin = readMixinClass(DroneEntityMixin.class);
        assertTrue(droneMixin.shadowMethods.isEmpty(),
                "DroneEntityMixin must not shadow inherited VehicleEntity methods");
        assertEquals(Set.of(controllerShadow), droneMixin.shadowFields);
        assertTrue(droneMixin.finalFields.contains(controllerShadow),
                "The CONTROLLER shadow must be decorated with @Final");
        assertTrue(readMixinClass(dev.sbwdronejammer.mixin.client.DroneEntityClientMixin.class)
                        .shadowMethods.isEmpty(),
                "DroneEntityClientMixin must not shadow inherited VehicleEntity methods");
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static ClassContract readJarClass(Path jar, String internalName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(internalName + ".class");
            assertNotNull(entry, "Missing production class " + internalName);
            try (InputStream input = zip.getInputStream(entry)) {
                ClassContract contract = new ClassContract();
                new ClassReader(input).accept(contract, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                return contract;
            }
        }
    }

    private static MixinContract readMixinClass(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            assertNotNull(input, "Missing compiled mixin " + type.getName());
            MixinContract contract = new MixinContract();
            new ClassReader(input).accept(contract, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            return contract;
        }
    }

    private static final class ClassContract extends ClassVisitor {
        private String superName;
        private final Set<String> methods = new HashSet<>();
        private final Set<String> fields = new HashSet<>();
        private final Map<String, Integer> fieldAccess = new HashMap<>();

        private ClassContract() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            this.superName = superName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            methods.add(name + descriptor);
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                                       Object value) {
            String field = name + ":" + descriptor;
            fields.add(field);
            fieldAccess.put(field, access);
            return null;
        }
    }

    private static final class MixinContract extends ClassVisitor {
        private final List<String> targets = new ArrayList<>();
        private final Map<String, String> invokersByTargetSignature = new HashMap<>();
        private final Set<String> shadowMethods = new HashSet<>();
        private final Set<String> shadowFields = new HashSet<>();
        private final Set<String> finalFields = new HashSet<>();

        private MixinContract() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
                return null;
            }
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitArray(String name) {
                    if (!"targets".equals(name)) {
                        return null;
                    }
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String name, Object value) {
                            targets.add((String) value);
                        }
                    };
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    if ("Lorg/spongepowered/asm/mixin/Shadow;".equals(annotation)) {
                        shadowMethods.add(name + descriptor);
                        return null;
                    }
                    if (!"Lorg/spongepowered/asm/mixin/gen/Invoker;".equals(annotation)) {
                        return null;
                    }
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String key, Object value) {
                            if ("value".equals(key)) {
                                invokersByTargetSignature.put(value + descriptor, (String) value);
                            }
                        }
                    };
                }
            };
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                                       Object value) {
            String field = name + ":" + descriptor;
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    if ("Lorg/spongepowered/asm/mixin/Shadow;".equals(annotation)) {
                        shadowFields.add(field);
                    }
                    if ("Lorg/spongepowered/asm/mixin/Final;".equals(annotation)) {
                        finalFields.add(field);
                    }
                    return null;
                }
            };
        }
    }
}
