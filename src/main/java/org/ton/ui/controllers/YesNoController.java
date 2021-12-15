package org.ton.ui.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.actions.MyLocalTon;
import org.ton.db.OrientDB;
import org.ton.db.entities.WalletEntity;
import org.ton.main.App;
import org.ton.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.nonNull;
import static org.ton.main.App.fxmlLoader;

@Slf4j
public class YesNoController implements Initializable {

    @FXML
    Label header;

    @FXML
    Label body;

    @FXML
    JFXButton okBtn;

    @FXML
    Label action;

    @FXML
    JFXTextField subWalletId;

    @FXML
    JFXTextField workchain;

    @FXML
    VBox inputFields;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        workchain.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\.\\-]+")) {
                workchain.setText(newValue.replaceAll("[^\\d\\.\\-]", ""));
            }
        });

        subWalletId.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\.]+")) {
                subWalletId.setText(newValue.replaceAll("[^\\d\\.]", ""));
            }
        });
    }

    public void okBtnAction() {
        log.debug("ok clicked, action {}", action.getText());
        MainController mainController = fxmlLoader.getController();

        switch (action.getText()) {
            case "reset":
                log.debug("do reset");
                mainController.yesNoDialog.close();
                //show msg
                doReset();
                break;
            case "transform":
                log.debug("do transform");
                mainController.yesNoDialog.close();
                break;
            case "create":
                log.debug("create");
                mainController.yesNoDialog.close();
                doCreateAccount();
                break;
            default:
                log.debug("no action");
                mainController.yesNoDialog.close();
        }
    }

    private void doCreateAccount() {
        App.mainController.showInfoMsg("Creating new wallet...", 3);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Thread.currentThread().setName("Create new wallet");
            OrientDB.getDB().activateOnCurrentThread();
            try {
                long chain = Long.parseLong(StringUtils.isEmpty(workchain.getText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultWorkChain()) : workchain.getText());
                long walletId = Long.parseLong(StringUtils.isEmpty(subWalletId.getText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultSubWalletId()) : subWalletId.getText());

                WalletEntity walletEntity = MyLocalTon.getInstance().createWalletEntity(MyLocalTon.getInstance().getSettings().getGenesisNode(), null, chain, walletId);
                if (nonNull(walletEntity)) {
                    App.mainController.showSuccessMsg("Wallet " + walletEntity.getFullAddress() + " created", 3);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void doReset() {
        try {
            MainController mainController = fxmlLoader.getController();
            mainController.saveSettings();
            Thread.sleep(100);
            if (isWindows()) {
                if (Utils.doShutdown()) {
                    log.info("restarting: cmd /c start java -jar {} restart", Utils.getMyPath());
                    Runtime.getRuntime().exec("cmd /c start java -jar " + Utils.getMyPath() + " restart");
                    System.exit(0);
                }
            } else {
                if (Utils.doShutdown()) {
                    // works on linux
                    log.info("restarting: java -jar {}", Utils.getMyPath());
                    Runtime.getRuntime().exec("java -jar " + Utils.getMyPath() + " restart");
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            log.error("Cannot restart MyLocalTon, error " + e.getMessage());
        }
    }
}
