package org.ton.ui.controllers;

import com.jfoenix.controls.*;
import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.actions.MyLocalTon;
import org.ton.db.OrientDB;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.WalletEntity;
import org.ton.executors.liteclient.LiteClientExecutor;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.BlockShortSeqno;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.ResultListBlockTransactions;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.main.App;
import org.ton.settings.MyLocalTonSettings;
import org.ton.utils.Utils;
import org.ton.wallet.WalletVersion;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static com.sun.javafx.PlatformUtil.isLinux;
import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.requireNonNull;
import static org.ton.actions.MyLocalTon.MAX_ROWS_IN_GUI;

@Slf4j
public class MainController implements Initializable {

    public static final String LIGHT_BLUE = "#dbedff";
    public static final String ORANGE = "orange";
    @FXML
    public StackPane superWindow;

    @FXML
    public BorderPane mainWindow;

    @FXML
    public JFXTabPane mainMenuTabs;

    @FXML
    public JFXTabPane settingTabs;

    @FXML
    public Label currentBlockNum;

    @FXML
    public Label liteClientInfo;

    @FXML
    public Label shardsNum;

    @FXML
    public ImageView scrollBtnImageView;

    @FXML
    public HBox topbar;

    @FXML
    public JFXListView<Node> blockslistviewid;

    @FXML
    public JFXListView<Node> transactionsvboxid;

    @FXML
    public JFXListView<Node> accountsvboxid;

    @FXML
    public TextField electedFor;

    @FXML
    public TextField initialBalance;

    @FXML
    public TextField globalId;

    @FXML
    public TextField electionStartBefore;

    @FXML
    public TextField electionEndBefore;

    @FXML
    public TextField stakesFrozenFor;

    @FXML
    public TextField gasPrice;

    @FXML
    public TextField cellPrice;

    @FXML
    public TextField nodeStateTtl;

    @FXML
    public TextField nodeBlockTtl;

    @FXML
    public TextField nodeArchiveTtl;

    @FXML
    public TextField nodeKeyProofTtl;

    @FXML
    public TextField nodeSyncBefore;

    @FXML
    public Tab settingsTab;

    @FXML
    public Tab accountsTab;

    @FXML
    public Tab transactionsTab;

    @FXML
    public JFXButton myLocalTonDbDirBtn;

    @FXML
    public Tab logsTab;

    @FXML
    JFXCheckBox shardStateCheckbox;

    @FXML
    JFXCheckBox showMsgBodyCheckBox;

    @FXML
    Tab searchTab;

    @FXML
    Label searchTabText;

    @FXML
    JFXTextField searchField;

    @FXML
    public Tab foundBlocks;

    @FXML
    public Tab foundAccounts;

    @FXML
    public Tab foundTxs;

    @FXML
    public JFXTabPane foundTabs;

    @FXML
    public JFXListView<Node> foundBlockslistviewid;

    @FXML
    public JFXListView<Node> foundTxsvboxid;

    @FXML
    public JFXListView<Node> foundAccountsvboxid;

    @FXML
    public Tab blocksTab;

    @FXML
    TextField nodePublicPort;

    @FXML
    TextField nodeConsolePort;

    @FXML
    TextField litesServerPort;

    @FXML
    TextField dhtServerPort;

    @FXML
    ImageView aboutLogo;

    @FXML
    JFXTextField gasPriceMc;

    @FXML
    JFXTextField cellPriceMc;

    @FXML
    JFXTextField maxFactor;

    @FXML
    JFXTextField minTotalStake;

    @FXML
    JFXTextField maxStake;

    @FXML
    JFXTextField minStake;

    @FXML
    JFXComboBox<String> walletVersion;

    @FXML
    Label statusBar;

    @FXML
    private JFXButton scrollBtn;

    @FXML
    private JFXSlider walletsNumber;

    @FXML
    private TextField coinsPerWallet;

    @FXML
    private TextField valLogDir;

    @FXML
    private TextField dhtLogDir;

    @FXML
    private TextField minValidators;

    @FXML
    private TextField maxValidators;

    @FXML
    private TextField maxMainValidators;

    @FXML
    private TextField myLocalTonLog;

    @FXML
    private TextField myLocalTonDbLogDir;

    @FXML
    public JFXCheckBox tickTockCheckBox;

    @FXML
    public JFXCheckBox mainConfigTxCheckBox;

    @FXML
    public JFXCheckBox inOutMsgsCheckBox;

    @FXML
    public Label dbSizeId;

    @FXML
    public ComboBox<String> myLogLevel;

    @FXML
    public ComboBox<String> tonLogLevel;

    private MyLocalTonSettings settings;

    JFXDialog sendDialog;
    JFXDialog yesNoDialog;

    public void showSendDialog(String srcAddr) throws IOException {

        Parent parent = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/dialogsend.fxml")).load();

        ((Label) parent.lookup("#hiddenWalletAddr")).setText(srcAddr);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        sendDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
        sendDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        sendDialog.close();
                    }
                }
        );
        sendDialog.setOnDialogOpened(jfxDialogEvent -> {
            parent.lookup("#destAddr").requestFocus();
        });
        sendDialog.show();
    }

    public void showInfoMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> {
            statusBar.setStyle("-fx-text-fill: black; -fx-background-color: dbedff");
            Rectangle rect = new Rectangle();
            rect.setFill(Color.valueOf(LIGHT_BLUE));
            statusBar.setBackground(new Background(new BackgroundFill(rect.getFill(), CornerRadii.EMPTY, Insets.EMPTY)));
            statusBar.setText(msg);
            animateBackgroundColor(statusBar, Color.valueOf(LIGHT_BLUE), Color.valueOf(LIGHT_BLUE), (int) (durationSeconds * 1000));
            animateFontColor(statusBar, Color.BLACK, Color.valueOf(LIGHT_BLUE), (int) (durationSeconds * 1000));
        });
    }

    public void showSuccessMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> {
            statusBar.setStyle("-fx-text-fill: white; -fx-background-color: green");
            Rectangle rect = new Rectangle();
            rect.setFill(Color.GREEN);
            statusBar.setBackground(new Background(new BackgroundFill(rect.getFill(), CornerRadii.EMPTY, Insets.EMPTY)));
            statusBar.setText(msg);

            animateBackgroundColor(statusBar, Color.GREEN, Color.valueOf(LIGHT_BLUE), (int) (durationSeconds * 1000));
            animateFontColor(statusBar, Color.WHITE, Color.valueOf(LIGHT_BLUE), (int) (durationSeconds * 1000));
        });
    }

    public void showErrorMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> {
            statusBar.setStyle("-fx-text-fill: black; -fx-background-color: lightcoral");
            Rectangle rect = new Rectangle();
            rect.setFill(Color.valueOf(LIGHT_BLUE));
            statusBar.setBackground(new Background(new BackgroundFill(rect.getFill(), CornerRadii.EMPTY, Insets.EMPTY)));
            statusBar.setText(msg);
            animateBackgroundColor(statusBar, Color.valueOf("lightcoral"), Color.valueOf(LIGHT_BLUE), (int) (durationSeconds * 1000));
            animateFontColor(statusBar, Color.BLACK, Color.valueOf(LIGHT_BLUE), (int) (durationSeconds * 1000));
        });
    }

    public void showWarningMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> {
            statusBar.setStyle("-fx-text-fill: black; -fx-background-color: orange");
            Rectangle rect = new Rectangle();
            rect.setFill(Color.valueOf(ORANGE));
            statusBar.setBackground(new Background(new BackgroundFill(rect.getFill(), CornerRadii.EMPTY, Insets.EMPTY)));
            statusBar.setText(msg);
        });
    }

    public void showShutdownMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> {
            statusBar.setStyle("-fx-text-fill: black; -fx-background-color: orange");
            Rectangle rect = new Rectangle();
            rect.setFill(Color.valueOf(ORANGE));
            statusBar.setBackground(new Background(new BackgroundFill(rect.getFill(), CornerRadii.EMPTY, Insets.EMPTY)));
            statusBar.setText(msg);

            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.schedule(() -> {
                log.info("final closing");
                Platform.exit(); // closes main form
                System.exit(0); // initiating shutdown hook
            }, 2, TimeUnit.SECONDS);
        });
    }

    public static void animateBackgroundColor(Control control, Color fromColor, Color toColor, int duration) {

        Rectangle rect = new Rectangle();
        rect.setFill(fromColor);

        Rectangle rectFont = new Rectangle();
        rectFont.setFill(Color.BLACK);

        FillTransition tr = new FillTransition();
        tr.setShape(rect);
        tr.setDuration(Duration.millis(1000));
        tr.setFromValue(fromColor);
        tr.setToValue(toColor);

        tr.setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                control.setBackground(new Background(new BackgroundFill(rect.getFill(), CornerRadii.EMPTY, Insets.EMPTY)));
                return t;
            }
        });
        tr.setDelay(Duration.millis(duration));
        tr.play();
    }

    public static void animateFontColor(Control control, Color fromColor, Color toColor, int duration) {

        Rectangle rect = new Rectangle();
        rect.setFill(fromColor);

        FillTransition tr = new FillTransition();
        tr.setShape(rect);
        tr.setDuration(Duration.millis(1000));
        tr.setFromValue(fromColor);
        tr.setToValue(toColor);

        tr.setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                ((Label) control).setTextFill(rect.getFill());
                return t;
            }
        });
        tr.setDelay(Duration.millis(duration));
        tr.play();
    }

    public void shutdown() {
        saveSettings();
    }

    @FXML
    void myLocalTonFileBtnAction() throws IOException {
        log.info("open mylocalton log {}", myLocalTonLog.getText().trim());
        if (isWindows()) {
            Runtime.getRuntime().exec("cmd /c start notepad " + myLocalTonLog.getText());
        } else {
            Runtime.getRuntime().exec("gio open " + myLocalTonLog.getText());
        }
    }

    @FXML
    void myLocalTonDbDirBtnAction() throws IOException {
        log.debug("open mylocalton db dir {}", myLocalTonDbLogDir.getText().trim());
        if (isWindows()) {
            Runtime.getRuntime().exec("cmd /c start " + myLocalTonDbLogDir.getText());
        } else {
            Runtime.getRuntime().exec("gio open " + myLocalTonDbLogDir.getText());
        }
    }

    @FXML
    void dhtLogDirBtnAction() throws IOException {
        log.debug("open dht dir {}", dhtLogDir.getText().trim());
        if (isWindows()) {
            Runtime.getRuntime().exec("cmd /c start " + dhtLogDir.getText());
        } else {
            Runtime.getRuntime().exec("gio open " + dhtLogDir.getText());
        }
    }

    @FXML
    void valLogDirBtnAction() throws IOException {
        log.debug("open validator log dir {}", valLogDir.getText().trim());
        if (isWindows()) {
            Runtime.getRuntime().exec("cmd /c start " + valLogDir.getText());
        } else {
            Runtime.getRuntime().exec("gio open " + valLogDir.getText());
        }
    }

    @FXML
    void blocksOnScroll(ScrollEvent event) {

        Node n1 = blockslistviewid.lookup(".scroll-bar");

        if (n1 instanceof ScrollBar) {
            ScrollBar bar = (ScrollBar) n1;

            if (event.getDeltaY() < 0 && bar.getValue() > 0) { // bottom reached
                Platform.runLater(() -> {

                    BorderPane bp = (BorderPane) blockslistviewid.getItems().get(blockslistviewid.getItems().size() - 1);
                    long lastSeqno = Long.parseLong(((Label) ((Node) bp).lookup("#seqno")).getText());
                    long wc = Long.parseLong(((Label) ((Node) bp).lookup("#wc")).getText());

                    long createdAt = Utils.datetimeToTimestamp(((Label) ((Node) bp).lookup("#createdat")).getText());

                    log.info("bottom reached, seqno {}, time {}, hwm {} ", lastSeqno, Utils.toUtcNoSpace(createdAt), MyLocalTon.getInstance().getBlocksScrollBarHighWaterMark().get());

                    if (lastSeqno == 1L && wc == -1L) {
                        return;
                    }

                    if (blockslistviewid.getItems().size() > MAX_ROWS_IN_GUI) {
                        showWarningMsg("Maximum amount (" + MyLocalTon.getInstance().getBlocksScrollBarHighWaterMark().get() + ") of visible blocks in GUI reached.", 5);
                        return;
                    }

                    OrientDB.getDB().activateOnCurrentThread();
                    List<BlockEntity> blocks = OrientDB.loadBlocksBefore(createdAt);
                    MyLocalTon.getInstance().getBlocksScrollBarHighWaterMark().addAndGet(blocks.size());

                    ObservableList<Node> blockRows = FXCollections.observableArrayList();

                    for (BlockEntity block : blocks) {
                        try {
                            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("blockrow.fxml"));
                            javafx.scene.Node blockRow = fxmlLoader.load();

                            ResultLastBlock resultLastBlock = ResultLastBlock.builder()
                                    .createdAt(block.getCreatedAt())
                                    .seqno(block.getSeqno())
                                    .rootHash(block.getRoothash())
                                    .fileHash(block.getFilehash())
                                    .wc(block.getWc())
                                    .shard(block.getShard())
                                    .build();

                            MyLocalTon.getInstance().populateBlockRowWithData(resultLastBlock, blockRow, null);

                            if (resultLastBlock.getWc() == -1L) {
                                blockRow.setStyle("-fx-background-color: e9f4ff;");
                            }
                            log.debug("Adding block {} roothash {}", block.getSeqno(), block.getRoothash());

                            blockRows.add(blockRow);

                        } catch (IOException e) {
                            log.error("Error loading blockrow.fxml file, {}", e.getMessage());
                            return;
                        }
                    }

                    log.debug("blockRows.size  {}", blockRows.size());

                    if ((blockRows.isEmpty()) && (lastSeqno < 10)) {
                        log.debug("On start some blocks were skipped, load them now from 1 to {}", lastSeqno - 1);

                        LongStream.range(1, lastSeqno).forEach(i -> { // TODO for loop big integer
                            try {
                                ResultLastBlock block = LiteClientParser.parseBySeqno(new LiteClientExecutor().executeBySeqno(MyLocalTon.getInstance().getSettings().getGenesisNode(), -1L, "8000000000000000", String.valueOf(i)));
                                log.debug("Load missing block {}: {}", i, block.getFullBlockSeqno());
                                MyLocalTon.getInstance().insertBlocksAndTransactions(MyLocalTon.getInstance().getSettings().getGenesisNode(), new LiteClientExecutor(), block, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    blockslistviewid.getItems().addAll(blockRows);
                });
            }
            if (event.getDeltaY() > 0) { // top reached
                log.debug("top reached");
            }
        }
    }

    @FXML
    void txsOnScroll(ScrollEvent event) {

        log.debug("txsOnScroll: {}", event);

        Node n1 = transactionsvboxid.lookup(".scroll-bar");

        if (n1 instanceof ScrollBar) {
            ScrollBar bar = (ScrollBar) n1;

            if (event.getDeltaY() < 0 && bar.getValue() > 0) { // bottom reached

                Platform.runLater(() -> {

                    BorderPane bp = (BorderPane) transactionsvboxid.getItems().get(transactionsvboxid.getItems().size() - 1);
                    String shortseqno = ((Label) ((Node) bp).lookup("#block")).getText();

                    long createdAt = Utils.datetimeToTimestamp(((Label) ((Node) bp).lookup("#time")).getText());

                    BlockShortSeqno blockShortSeqno = BlockShortSeqno.builder()
                            .wc(Long.valueOf(StringUtils.substringBetween(shortseqno, "(", ",")))
                            .shard(StringUtils.substringBetween(shortseqno, ",", ","))
                            .seqno(StringUtils.substring(StringUtils.substringAfterLast(shortseqno, ","), 0, -1))
                            .build();

                    log.debug("bottom reached, seqno {}, hwm {}, createdAt {}, utc {}", blockShortSeqno.getSeqno(), MyLocalTon.getInstance().getTxsScrollBarHighWaterMark().get(), createdAt, Utils.toUtcNoSpace(createdAt));

                    if (new BigInteger(blockShortSeqno.getSeqno()).compareTo(BigInteger.ZERO) == 0L) {
                        return;
                    }

                    if (transactionsvboxid.getItems().size() > MAX_ROWS_IN_GUI) {
                        showWarningMsg("Maximum amount (" + MyLocalTon.getInstance().getTxsScrollBarHighWaterMark().get() + ") of visible TXs in GUI reached.", 5);
                        return;
                    }

                    OrientDB.getDB().activateOnCurrentThread();
                    List<TxEntity> txs = OrientDB.loadTxsBefore(createdAt);

                    MyLocalTon.getInstance().applyTxGuiFilters(txs);

                    MyLocalTon.getInstance().getTxsScrollBarHighWaterMark().addAndGet(txs.size());

                    ObservableList<Node> txRows = FXCollections.observableArrayList();

                    for (TxEntity txEntity : txs) {
                        try {
                            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("txrow.fxml"));
                            javafx.scene.Node txRow = fxmlLoader.load();

                            String shortBlock = String.format("(%d,%s,%s)", txEntity.getWc(), txEntity.getShard(), txEntity.getSeqno());

                            ResultListBlockTransactions resultListBlockTransactions = ResultListBlockTransactions.builder()
                                    .txSeqno(txEntity.getSeqno())
                                    .hash(txEntity.getTxHash())
                                    .accountAddress(txEntity.getTx().getAccountAddr())
                                    .lt(txEntity.getTx().getLt())
                                    .build();

                            Transaction txDetails = Transaction.builder()
                                    .accountAddr(txEntity.getTx().getAccountAddr())
                                    .description(txEntity.getTx().getDescription())
                                    .inMsg(txEntity.getTx().getInMsg())
                                    .endStatus(txEntity.getTx().getEndStatus())
                                    .now(txEntity.getTx().getNow())
                                    .totalFees(txEntity.getTx().getTotalFees())
                                    .lt(txEntity.getTxLt())
                                    .build();

                            MyLocalTon.getInstance().populateTxRowWithData(shortBlock, resultListBlockTransactions, txDetails, txRow, txEntity);

                            if (txEntity.getTypeTx().equals("Message")) {
                                txRow.setStyle("-fx-background-color: e9f4ff;");
                            }

                            log.debug("adding tx hash {}, addr {}", txEntity.getTxHash(), txEntity.getTx().getAccountAddr());

                            txRows.add(txRow);

                        } catch (IOException e) {
                            log.error("error loading txrow.fxml file, {}", e.getMessage());
                            return;
                        }
                    }
                    log.debug("txRows.size  {}", txRows.size());

                    if ((txRows.isEmpty()) && (new BigInteger(blockShortSeqno.getSeqno()).compareTo(BigInteger.TEN) < 0)) {
                        log.debug("on start some blocks were skipped and thus some transactions get lost, load them from blocks 1");

                        LongStream.range(1, Long.parseLong(blockShortSeqno.getSeqno())).forEach(i -> {
                            try {
                                ResultLastBlock block = LiteClientParser.parseBySeqno(new LiteClientExecutor().executeBySeqno(MyLocalTon.getInstance().getSettings().getGenesisNode(), -1L, "8000000000000000", String.valueOf(i)));
                                log.debug("load missing block {}: {}", i, block.getFullBlockSeqno());
                                MyLocalTon.getInstance().insertBlocksAndTransactions(MyLocalTon.getInstance().getSettings().getGenesisNode(), new LiteClientExecutor(), block, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    transactionsvboxid.getItems().addAll(txRows);
                });
            }
            if (event.getDeltaY() > 0) { // top reached
                log.debug("top reached");
            }
        }
    }

    @FXML
    void scrollBtnAction() {
        MyLocalTon.getInstance().setAutoScroll(!MyLocalTon.getInstance().getAutoScroll());

        if (Boolean.TRUE.equals(MyLocalTon.getInstance().getAutoScroll())) {
            scrollBtnImageView.setImage(new Image(requireNonNull(getClass().getResourceAsStream("/org/ton/images/scroll.png"))));
        } else {
            scrollBtnImageView.setImage(new Image(requireNonNull(getClass().getResourceAsStream("/org/ton/images/scrolloff.png"))));
        }
        log.debug("auto scroll {}", MyLocalTon.getInstance().getAutoScroll());
    }

    private void showLoading(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        //stage.initStyle(StageStyle.TRANSPARENT);
        //stage.setFill(Color.TRANSPARENT);
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("modal_progress" + ".fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        stage.setTitle("My modal window");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(((Node) event.getSource()).getScene().getWindow());
        stage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        settings = MyLocalTon.getInstance().getSettings();

        walletsNumber.setOnMouseReleased(event -> {
            log.debug("walletsNumber released, {}", walletsNumber.getValue());
        });

        settingTabs.getSelectionModel().selectedItemProperty().addListener(e -> {
            log.debug("settings tab changed, save settings");
            saveSettings();
        });

        mainMenuTabs.getSelectionModel().selectedItemProperty().addListener(e -> {
            log.debug("main menu changed, save settings");
            saveSettings();
        });

        EventHandler<KeyEvent> onlyDigits = keyEvent -> {
            if (!((TextField) keyEvent.getSource()).getText().matches("[\\d\\.\\-]+")) {
                ((TextField) keyEvent.getSource()).setText(((TextField) keyEvent.getSource()).getText().replaceAll("[^\\d\\.\\-]", ""));
            }
        };

        coinsPerWallet.setOnKeyTyped(onlyDigits);

        nodePublicPort.setOnKeyTyped(onlyDigits);
        nodeConsolePort.setOnKeyTyped(onlyDigits);
        litesServerPort.setOnKeyTyped(onlyDigits);
        dhtServerPort.setOnKeyTyped(onlyDigits);

        globalId.setOnKeyTyped(onlyDigits);
        initialBalance.setOnKeyTyped(onlyDigits);
        maxMainValidators.setOnKeyTyped(onlyDigits);
        minValidators.setOnKeyTyped(onlyDigits);
        maxValidators.setOnKeyTyped(onlyDigits);
        electedFor.setOnKeyTyped(onlyDigits);
        electionStartBefore.setOnKeyTyped(onlyDigits);
        electionEndBefore.setOnKeyTyped(onlyDigits);
        stakesFrozenFor.setOnKeyTyped(onlyDigits);
        gasPrice.setOnKeyTyped(onlyDigits);
        gasPriceMc.setOnKeyTyped(onlyDigits);
        cellPrice.setOnKeyTyped(onlyDigits);
        cellPriceMc.setOnKeyTyped(onlyDigits);
        minStake.setOnKeyTyped(onlyDigits);
        maxStake.setOnKeyTyped(onlyDigits);
        minTotalStake.setOnKeyTyped(onlyDigits);
        maxFactor.setOnKeyTyped(onlyDigits);
        electionEndBefore.setOnKeyTyped(onlyDigits);
        nodeStateTtl.setOnKeyTyped(onlyDigits);
        nodeBlockTtl.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl.setOnKeyTyped(onlyDigits);
        nodeSyncBefore.setOnKeyTyped(onlyDigits);

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                log.debug("search for {}", searchField.getText());

                foundBlockslistviewid.getItems().clear();
                foundTxsvboxid.getItems().clear();
                foundAccountsvboxid.getItems().clear();

                //clear previous results
                mainMenuTabs.getTabs().add(searchTab);
                mainMenuTabs.getSelectionModel().selectLast();
                foundTabs.getTabs().add(foundBlocks);
                foundTabs.getTabs().add(foundAccounts);
                foundTabs.getTabs().add(foundTxs);

                String searchFor = searchField.getText();

                OrientDB.getDB().activateOnCurrentThread();
                List<BlockEntity> foundBlocks = OrientDB.searchBlocks(searchFor);
                MyLocalTon.getInstance().showFoundBlocksInGui(foundBlocks, searchFor);

                List<TxEntity> foundTxs = OrientDB.searchTxs(searchFor);
                MyLocalTon.getInstance().showFoundTxsInGui(foundTxs, searchFor);

                List<WalletEntity> foundAccs = OrientDB.searchAccounts(searchFor);
                MyLocalTon.getInstance().showFoundAccountsInGui(foundAccs, searchFor);
            }
        });

        mainMenuTabs.getTabs().remove(searchTab);

        foundTabs.getTabs().remove(foundBlocks);
        foundTabs.getTabs().remove(foundAccounts);
        foundTabs.getTabs().remove(foundTxs);

        scrollBtn.setTooltip(new Tooltip("Autoscroll on/off"));

        tickTockCheckBox.setSelected(settings.getUiSettings().isShowTickTockTransactions());
        mainConfigTxCheckBox.setSelected(settings.getUiSettings().isShowMainConfigTransactions());
        inOutMsgsCheckBox.setSelected(settings.getUiSettings().isShowInOutMessages());
        showMsgBodyCheckBox.setSelected(settings.getUiSettings().isShowBodyInMessage());
        shardStateCheckbox.setSelected(settings.getUiSettings().isShowShardStateInBlockDump());

        walletsNumber.setValue(settings.getWalletSettings().getNumberOfPreinstalledWallets());
        coinsPerWallet.setText(settings.getWalletSettings().getInitialAmount().toString());
        walletVersion.getItems().add(MyLocalTon.walletVersions.get("V1"));
        walletVersion.getItems().add(MyLocalTon.walletVersions.get("V2"));
        walletVersion.getItems().add(MyLocalTon.walletVersions.get("V3"));
        walletVersion.getSelectionModel().select(MyLocalTon.walletVersions.get(settings.getWalletSettings().getWalletVersion()));

        valLogDir.setText(settings.getGenesisNode().getTonDbDir());
        myLocalTonLog.setText(settings.LOG_FILE);
        myLocalTonDbLogDir.setText(settings.DB_DIR);
        dhtLogDir.setText(settings.getGenesisNode().getDhtServerDir());

        minValidators.setText(settings.getBlockchainSettings().getMinValidators().toString());
        maxValidators.setText(settings.getBlockchainSettings().getMaxValidators().toString());
        maxMainValidators.setText(settings.getBlockchainSettings().getMaxMainValidators().toString());

        electedFor.setText(settings.getBlockchainSettings().getElectedFor().toString());
        electionStartBefore.setText(settings.getBlockchainSettings().getElectionStartBefore().toString());
        electionEndBefore.setText(settings.getBlockchainSettings().getElectionEndBefore().toString());
        stakesFrozenFor.setText(settings.getBlockchainSettings().getElectionStakesFrozenFor().toString());

        globalId.setText(settings.getBlockchainSettings().getGlobalId().toString());
        initialBalance.setText(settings.getBlockchainSettings().getInitialBalance().toString());
        gasPrice.setText(settings.getBlockchainSettings().getGasPrice().toString());
        gasPriceMc.setText(settings.getBlockchainSettings().getGasPriceMc().toString());
        cellPrice.setText(settings.getBlockchainSettings().getCellPrice().toString());
        cellPriceMc.setText(settings.getBlockchainSettings().getCellPriceMc().toString());

        minStake.setText(settings.getBlockchainSettings().getMinValidatorStake().toString());
        maxStake.setText(settings.getBlockchainSettings().getMaxValidatorStake().toString());
        minTotalStake.setText(settings.getBlockchainSettings().getMinTotalValidatorStake().toString());
        maxFactor.setText(settings.getBlockchainSettings().getMaxFactor().toString());

        nodeBlockTtl.setText(settings.getBlockchainSettings().getValidatorBlockTtl().toString());
        nodeArchiveTtl.setText(settings.getBlockchainSettings().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl.setText(settings.getBlockchainSettings().getValidatorKeyProofTtl().toString());
        nodeStateTtl.setText(settings.getBlockchainSettings().getValidatorStateTtl().toString());
        nodeSyncBefore.setText(settings.getBlockchainSettings().getValidatorSyncBefore().toString());

        nodePublicPort.setText(settings.getGenesisNode().getPublicPort().toString());
        nodeConsolePort.setText(settings.getGenesisNode().getConsolePort().toString());
        litesServerPort.setText(settings.getGenesisNode().getLiteServerPort().toString());
        dhtServerPort.setText(settings.getGenesisNode().getDhtPort().toString());

        tonLogLevel.getItems().add("DEBUG");
        tonLogLevel.getItems().add("WARNING");
        tonLogLevel.getItems().add("INFO");
        tonLogLevel.getItems().add("ERROR");
        tonLogLevel.getItems().add("FATAL");
        tonLogLevel.getSelectionModel().select(settings.getLogSettings().getTonLogLevel());

        myLogLevel.getItems().add("INFO");
        myLogLevel.getItems().add("DEBUG");
        myLogLevel.getItems().add("ERROR");
        myLogLevel.getSelectionModel().select(settings.getLogSettings().getMyLocalTonLogLevel());
    }

    void saveSettings() {
        log.debug("saving all settings");
        settings.getUiSettings().setShowTickTockTransactions(tickTockCheckBox.isSelected());
        settings.getUiSettings().setShowMainConfigTransactions(mainConfigTxCheckBox.isSelected());
        settings.getUiSettings().setShowInOutMessages(inOutMsgsCheckBox.isSelected());
        settings.getUiSettings().setShowBodyInMessage(showMsgBodyCheckBox.isSelected());
        settings.getUiSettings().setShowShardStateInBlockDump(shardStateCheckbox.isSelected());

        settings.getWalletSettings().setNumberOfPreinstalledWallets((long) walletsNumber.getValue());
        settings.getWalletSettings().setInitialAmount(Long.valueOf(coinsPerWallet.getText()));
        settings.getWalletSettings().setWalletVersion(WalletVersion.getKeyByValueInMap(walletVersion.getValue()));

        settings.getBlockchainSettings().setMinValidators(Long.valueOf(minValidators.getText()));
        settings.getBlockchainSettings().setMaxValidators(Long.valueOf(maxValidators.getText()));
        settings.getBlockchainSettings().setMaxMainValidators(Long.valueOf(maxMainValidators.getText()));

        settings.getBlockchainSettings().setGlobalId(Long.valueOf(globalId.getText()));
        settings.getBlockchainSettings().setInitialBalance(Long.valueOf(initialBalance.getText()));

        settings.getBlockchainSettings().setElectedFor(Long.valueOf(electedFor.getText()));
        settings.getBlockchainSettings().setElectionStartBefore(Long.valueOf(electionStartBefore.getText()));
        settings.getBlockchainSettings().setElectionEndBefore(Long.valueOf(electionEndBefore.getText()));
        settings.getBlockchainSettings().setElectionStakesFrozenFor(Long.valueOf(stakesFrozenFor.getText()));
        settings.getBlockchainSettings().setGasPrice(Long.valueOf(gasPrice.getText()));
        settings.getBlockchainSettings().setGasPriceMc(Long.valueOf(gasPriceMc.getText()));
        settings.getBlockchainSettings().setCellPrice(Long.valueOf(cellPrice.getText()));
        settings.getBlockchainSettings().setCellPriceMc(Long.valueOf(cellPriceMc.getText()));

        settings.getBlockchainSettings().setMinValidatorStake(Long.valueOf(minStake.getText()));
        settings.getBlockchainSettings().setMaxValidatorStake(Long.valueOf(maxStake.getText()));
        settings.getBlockchainSettings().setMinTotalValidatorStake(Long.valueOf(minTotalStake.getText()));
        settings.getBlockchainSettings().setMaxFactor(Long.valueOf(maxFactor.getText()));

        settings.getBlockchainSettings().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl.getText()));
        settings.getBlockchainSettings().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl.getText()));
        settings.getBlockchainSettings().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl.getText()));
        settings.getBlockchainSettings().setValidatorStateTtl(Long.valueOf(nodeStateTtl.getText()));
        settings.getBlockchainSettings().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore.getText()));

        settings.getLogSettings().setTonLogLevel(tonLogLevel.getValue());
        settings.getLogSettings().setMyLocalTonLogLevel(myLogLevel.getValue());

        settings.getGenesisNode().setPublicPort(Integer.valueOf(nodePublicPort.getText()));
        settings.getGenesisNode().setConsolePort(Integer.valueOf(nodeConsolePort.getText()));
        settings.getGenesisNode().setLiteServerPort(Integer.valueOf(litesServerPort.getText()));
        settings.getGenesisNode().setDhtPort(Integer.valueOf(dhtServerPort.getText()));

        settings.saveSettingsToGson(settings);
    }

    public void accountsOnScroll(ScrollEvent scrollEvent) {
        log.debug("accountsOnScroll");
    }

    public void foundBlocksOnScroll(ScrollEvent scrollEvent) {
        log.debug("foundBlocksOnScroll");
    }

    public void foundTxsOnScroll(ScrollEvent scrollEvent) {
        log.debug("foundTxsOnScroll");
    }

    public void liteServerClicked() throws IOException {
        String lastCommand = new LiteClientExecutor(false).getLastCommand(MyLocalTon.getInstance().getSettings().getGenesisNode());
        log.info("show console with last command, {}", lastCommand);

        if (isWindows()) {
            log.info("cmd /c start cmd.exe /k \"echo " + lastCommand + " && " + lastCommand + "\"");
            Runtime.getRuntime().exec("cmd /c start cmd.exe /k \"echo " + lastCommand + " && " + lastCommand + "\"");
        } else if (isLinux()) {
            if (Files.exists(Paths.get("/usr/bin/xterm"))) {
                log.info("/usr/bin/xterm -hold -geometry 200 -e " + lastCommand);
                Runtime.getRuntime().exec("/usr/bin/xterm -hold -geometry 200 -e " + lastCommand);
            } else {
                log.info("xterm is not installed");
            }
        } else {
            //log.info("zsh -c \"" + lastCommand + "\"");
            //Runtime.getRuntime().exec("zsh -c \"" + lastCommand + "\"");
            log.debug("terminal call not implemented");
        }

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(lastCommand);
        clipboard.setContent(content);
        log.debug(lastCommand + " copied");
        App.mainController.showInfoMsg("lite-client last command copied to clipboard", 0.5);
    }

    public void resetAction() throws IOException {
        log.info("reset");

        Parent parent = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/yesnodialog.fxml")).load();
        parent.lookup("#inputFields").setVisible(false);
        parent.lookup("#body").setVisible(true);
        parent.lookup("#header").setVisible(true);
        ((Label) parent.lookup("#action")).setText("reset");
        ((Label) parent.lookup("#header")).setText("Reset TON blockchain");
        ((Label) parent.lookup("#body")).setText("You can reset current single-node TON blockchain to the new settings. All data will be lost and zero state will be created from scratch. Do you want to proceed?");
        parent.lookup("#okBtn").setDisable(false);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
        yesNoDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        yesNoDialog.close();
                    }
                }
        );
        yesNoDialog.setOnDialogOpened(jfxDialogEvent -> {
            //parent.lookup("#destAddr").requestFocus();
        });
        yesNoDialog.show();
    }

    public void transformAction(ActionEvent actionEvent) throws IOException {
        log.info("transform");

        Parent parent = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/yesnodialog.fxml")).load();
        parent.lookup("#inputFields").setVisible(false);
        parent.lookup("#body").setVisible(true);
        parent.lookup("#header").setVisible(true);
        ((Label) parent.lookup("#action")).setText("transform");
        ((Label) parent.lookup("#header")).setText("Transform");
        ((Label) parent.lookup("#body")).setText("You can transform this single-node TON blockchain into three-nodes TON blockchain, where all three nodes will act as validators and participate in elections. " +
                "Later you will be able to add more full nodes if you wish. Do you want to proceed?");
        parent.lookup("#okBtn").setDisable(true);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
        yesNoDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        yesNoDialog.close();
                    }
                }
        );
        yesNoDialog.setOnDialogOpened(jfxDialogEvent -> {
            //parent.lookup("#destAddr").requestFocus();
        });
        yesNoDialog.show();
    }

    public void createNewAccountBtn() throws IOException {
        log.info("create account btn");

        Parent parent = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/yesnodialog.fxml")).load();
        ((Label) parent.lookup("#action")).setText("create");
        ((Label) parent.lookup("#header")).setText("Create " + settings.getWalletSettings().getWalletVersion());
        parent.lookup("#body").setVisible(false);
        parent.lookup("#inputFields").setVisible(true);
        if (settings.getWalletSettings().getWalletVersion().equals("V3")) {
            parent.lookup("#workchain").setVisible(true);
            parent.lookup("#subWalletId").setVisible(true);
        } else {
            parent.lookup("#workchain").setVisible(true);
            parent.lookup("#subWalletId").setVisible(false);
        }
        parent.lookup("#okBtn").setDisable(false);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
        yesNoDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        yesNoDialog.close();
                    }
                }
        );
        yesNoDialog.setOnDialogOpened(jfxDialogEvent -> {
            //parent.lookup("#destAddr").requestFocus();
        });
        yesNoDialog.show();

    }
}
