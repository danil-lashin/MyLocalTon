package org.ton.utils;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang3.SystemUtils;
import org.ton.settings.MyLocalTonSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
public class Extractor {
    private static final String CURRENT_DIR = System.getProperty("user.dir");

    public static final String MY_LOCAL_TON_ROOT_DIR = CURRENT_DIR + File.separator + "myLocalTon" + File.separator;
    public static final String SMARTCONT = "smartcont";
    public static final String BIN = "bin";
    public static final String TEMPLATES = "templates";
    public static final String UTILS = "utils";
    public static final String DB = "db";

    private final String nodeName;

    public Extractor(String pNodeName) throws IOException {
        log.info("Working Directory = " + CURRENT_DIR);
        nodeName = pNodeName;
        extractBinaries();
    }

    private void extractBinaries() throws IOException {
        synchronized (Extractor.class) {

            if (Files.notExists(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN), LinkOption.NOFOLLOW_LINKS)) {

                log.info("Detected OS: {}", System.getProperty("os.name"));
                Files.createDirectories(Paths.get(MyLocalTonSettings.DB_DIR));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "wallets"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "zerostate"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "certs"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "import"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "static"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "keyring"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "log"));

                if (SystemUtils.IS_OS_WINDOWS) {
                    extractWindowsBinaries();
                } else if (SystemUtils.IS_OS_LINUX) {
                    if (Utils.getUbuntuVersion().equals("20.04")) {
                        extractUbuntuBinaries("ubuntu20");
                    } else if (Utils.getUbuntuVersion().equals("20.04-arm64")) {
                        extractUbuntuBinaries("ubuntu20-arm64");
                    } else if (Utils.getUbuntuVersion().equals("18.04")) {
                        extractUbuntuBinaries("ubuntu18");
                    } else if (Utils.getUbuntuVersion().equals("18.04-arm64")) {
                        extractUbuntuBinaries("ubuntu18-arm64");
                    } else if (Utils.getUbuntuVersion().equals("22.04")) {
                        extractUbuntuBinaries("ubuntu22");
                    } else if (Utils.getUbuntuVersion().equals("22.04-arm64")) {
                        extractUbuntuBinaries("ubuntu22-arm64");
                    } else {
                        log.error("This Linux system officially is not supported, but let's try to run TON binaries compiled on Ubuntu 20.04");
                        extractUbuntuBinaries("ubuntu20");
                    }
                } else if (SystemUtils.IS_OS_MAC) {
                    if (Utils.isMacOsArm()) {
                        extractMacBinaries("macos-arm64.zip");
                    } else {
                        extractMacBinaries("macos.zip");
                    }
                } else {
                    log.error("You are running neither on Windows nor Unix nor MacOS. We don't have compiled client for it.");
                    System.exit(0); // initiating shutdown hook
                }

                // extract other cross-platform files
                InputStream readGlobalConfig = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/global.config.json");
                Files.copy(readGlobalConfig, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "global.config.json"), StandardCopyOption.REPLACE_EXISTING);
                readGlobalConfig.close();

                InputStream controlTemlate = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/control.template");
                Files.copy(controlTemlate, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "control.template"), StandardCopyOption.REPLACE_EXISTING);
                controlTemlate.close();

                InputStream globalConfigTemplate = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/ton-private-testnet.config.json.template");
                Files.copy(globalConfigTemplate, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "ton-private-testnet.config.json.template"), StandardCopyOption.REPLACE_EXISTING);
                globalConfigTemplate.close();

                InputStream exampleConfigJson = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/example.config.json");
                Files.copy(exampleConfigJson, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "example.config.json"), StandardCopyOption.REPLACE_EXISTING);
                exampleConfigJson.close();

                InputStream dbConfig = Extractor.class.getClassLoader().getResourceAsStream("org/ton/db/objectsdb.conf");
                Files.copy(dbConfig, Paths.get(MyLocalTonSettings.DB_DIR + File.separator + "objectsdb.conf"), StandardCopyOption.REPLACE_EXISTING);
                dbConfig.close();

                //extract patched cross-platform files
                InputStream newWallet = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/new-wallet.fif");
                Files.copy(newWallet, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "new-wallet.fif"), StandardCopyOption.REPLACE_EXISTING);
                newWallet.close();

                InputStream newWalletV2 = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/new-wallet-v2.fif");
                Files.copy(newWalletV2, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "new-wallet-v2.fif"), StandardCopyOption.REPLACE_EXISTING);
                newWalletV2.close();

                InputStream newWalletV3 = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/new-wallet-v3.fif");
                Files.copy(newWalletV3, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "new-wallet-v3.fif"), StandardCopyOption.REPLACE_EXISTING);
                newWalletV3.close();

                InputStream convertAddr = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/convert-addr.fif");
                Files.copy(convertAddr, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "convert-addr.fif"), StandardCopyOption.REPLACE_EXISTING);
                convertAddr.close();

                InputStream showAddr = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/show-addr.fif");
                Files.copy(showAddr, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "show-addr.fif"), StandardCopyOption.REPLACE_EXISTING);
                showAddr.close();

                InputStream genZeroStateFif = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/gen-zerostate.fif");
                Files.copy(genZeroStateFif, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "gen-zerostate.fif"), StandardCopyOption.REPLACE_EXISTING);
                genZeroStateFif.close();

                InputStream createSendTonCoinsQuery = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/create-send-grams-query.fif");
                Files.copy(createSendTonCoinsQuery, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "create-send-grams-query.fif"), StandardCopyOption.REPLACE_EXISTING);
                createSendTonCoinsQuery.close();

            } else {
                log.info("Binaries already extracted.");
            }
        }
    }

    private void extractWindowsUtils() {
        try {
            if (!Files.exists(Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "du.exe"), LinkOption.NOFOLLOW_LINKS)) {
                InputStream sendSignalCtrlC64 = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/SendSignalCtrlC64.exe");
                Files.copy(sendSignalCtrlC64, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "SendSignalCtrlC64.exe"), StandardCopyOption.REPLACE_EXISTING);
                sendSignalCtrlC64.close();

                InputStream du = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/du.exe");
                Files.copy(du, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "du.exe"), StandardCopyOption.REPLACE_EXISTING);
                du.close();

                InputStream cygWinDll = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/cygwin1.dll");
                Files.copy(cygWinDll, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygwin1.dll"), StandardCopyOption.REPLACE_EXISTING);
                cygWinDll.close();

                InputStream cygIntlDll = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/cygintl-8.dll");
                Files.copy(cygIntlDll, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygintl-8.dll"), StandardCopyOption.REPLACE_EXISTING);
                cygIntlDll.close();

                InputStream cygIconDll = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/cygiconv-2.dll");
                Files.copy(cygIconDll, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygiconv-2.dll"), StandardCopyOption.REPLACE_EXISTING);
                cygIconDll.close();
            }
        } catch (Exception e) {
            log.error("Error extracting windows utils, might be in use");
        }
    }

    private void extractWindowsBinaries() throws IOException {
        log.info("extracting windows.zip on windows");

        InputStream windowsBinaries = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/windows.zip");
        Files.copy(windowsBinaries, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "windows.zip"), StandardCopyOption.REPLACE_EXISTING);
        windowsBinaries.close();
        ZipFile zipFile = new ZipFile(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "windows.zip");
        zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
        Files.delete(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "windows.zip"));

//        log.debug("copy patched validator-engine.exe");
//        InputStream winValidatorEngine = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/validator-engine.exe");
//        Files.copy(winValidatorEngine, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine.exe"), StandardCopyOption.REPLACE_EXISTING);
//        winValidatorEngine.close();

        log.debug("windows binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);

        extractWindowsUtils();
    }

    private void extractUbuntuBinaries(String platform) throws IOException {
        log.info("extracting " + platform + " on linux");

        InputStream windowsBinaries = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/" + platform + ".zip");
        Files.copy(windowsBinaries, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform + ".zip"), StandardCopyOption.REPLACE_EXISTING);
        windowsBinaries.close();
        ZipFile zipFile = new ZipFile(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform + ".zip");
        zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
        Files.delete(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform + ".zip"));

//        log.debug("copy patched validator-engine");
//        InputStream ubuntuValidatorEngine = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/validator-engine-" + platform);
//        Files.copy(ubuntuValidatorEngine, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine"), StandardCopyOption.REPLACE_EXISTING);
//        ubuntuValidatorEngine.close();

        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-hardfork").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-state").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "dht-server").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "fift").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "func").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "generate-random-id").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "lite-client").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine-console").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "blockchain-explorer").start();

        log.debug("ubuntu binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
    }

    private void extractMacBinaries(String platform) throws IOException {
        log.info("extracting " + platform + " on macos");

        InputStream windowsBinaries = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/" + platform);
        Files.copy(windowsBinaries, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform), StandardCopyOption.REPLACE_EXISTING);
        windowsBinaries.close();
        ZipFile zipFile = new ZipFile(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform);
        zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
        Files.delete(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform));

//        log.debug("copy patched validator-engine");
//        InputStream macOsValidatorEngine = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/validator-engine-macos");
//        Files.copy(macOsValidatorEngine, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + "bin" + File.separator + "validator-engine"), StandardCopyOption.REPLACE_EXISTING);
//        macOsValidatorEngine.close();

        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-hardfork").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-state").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "dht-server").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "fift").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "func").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "generate-random-id").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "lite-client").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine-console").start();
        new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "blockchain-explorer").start();

        log.debug("mac binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
    }
}
