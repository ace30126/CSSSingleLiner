package csssingleliner;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

public class FileDropHandler extends TransferHandler {

    private CSSSingleLiner mainApp; // 메인 앱 참조

    public FileDropHandler(CSSSingleLiner app) {
        this.mainApp = app;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        // 드롭된 데이터가 파일 목록인지 확인
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    @SuppressWarnings("unchecked") // Transferable.getTransferData 결과 캐스팅 경고 무시
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        Transferable transferable = support.getTransferable();
        try {
            // 드롭된 파일 목록 가져오기
            List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

            if (!files.isEmpty()) {
                // 여러 파일이 드롭되어도 첫 번째 파일만 처리
                File droppedFile = files.get(0);
                // 메인 앱의 파일 처리 메서드 호출
                mainApp.processFile(droppedFile);
                return true; // 데이터 가져오기 성공
            }
        } catch (Exception e) {
            // 오류 처리 (예: 지원하지 않는 데이터 타입, 입출력 오류)
            e.printStackTrace();
             JOptionPane.showMessageDialog(mainApp, "파일을 가져오는 중 오류 발생:\n" + e.getMessage(), "드롭 오류", JOptionPane.ERROR_MESSAGE);
        }

        return false; // 데이터 가져오기 실패
    }
}