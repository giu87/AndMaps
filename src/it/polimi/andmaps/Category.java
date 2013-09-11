package it.polimi.andmaps;

public class Category {

	private int id;
	private String name;
	private boolean active;
	private OverlayItemCollection linkedOverlay;

	public Category(int id, String name, boolean active, OverlayItemCollection linkedOverlay) {

		this.id = id;
		this.name = name;
		this.active = active;
		this.linkedOverlay = linkedOverlay;

	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public OverlayItemCollection getLinkedOverlay() {
		return linkedOverlay;
	}

}