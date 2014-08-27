/**
 * 
 */
package ro.ciubex.dscautorename.model;

/**
 * @author Claudiu
 * 
 */
public class FolderItem {
	private String mFolderName;
	private boolean mSelected;

	/**
	 * 
	 */
	public FolderItem(String folderName) {
		mFolderName = folderName;
	}

	public boolean isSelected() {
		return mSelected;
	}

	public void setSelected(boolean selected) {
		this.mSelected = selected;
	}

	@Override
	public String toString() {
		return mFolderName;
	}
}
