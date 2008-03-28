/*
 * Copyright 1997-1999 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package javax.swing.text.html;

import java.util.Enumeration;
import java.awt.*;
import javax.swing.text.*;

/**
 * A view implementation to display an html list
 *
 * @author  Timothy Prinzing
 * @version 1.35 05/05/07
 */
public class ListView extends BlockView  {

    /**
     * Creates a new view that represents a list element.
     *
     * @param elem the element to create a view for
     */
    public ListView(Element elem) {
	super(elem, View.Y_AXIS);
    }

    /**
     * Calculates the desired shape of the list.
     *
     * @return the desired span
     * @see View#getPreferredSpan
     */
    public float getAlignment(int axis) {
        switch (axis) {
        case View.X_AXIS:
            return 0.5f;
        case View.Y_AXIS:
            return 0.5f;
        default:
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }

    /**
     * Renders using the given rendering surface and area on that
     * surface.
     *
     * @param g the rendering surface to use
     * @param allocation the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape allocation) {
	super.paint(g, allocation);
	Rectangle alloc = allocation.getBounds();
	Rectangle clip = g.getClipBounds();
	// Since listPainter paints in the insets we have to check for the
	// case where the child is not painted because the paint region is
	// to the left of the child. This assumes the ListPainter paints in
	// the left margin.
	if ((clip.x + clip.width) < (alloc.x + getLeftInset())) {
	    Rectangle childRect = alloc;
	    alloc = getInsideAllocation(allocation);
	    int n = getViewCount();
	    int endY = clip.y + clip.height;
	    for (int i = 0; i < n; i++) {
		childRect.setBounds(alloc);
		childAllocation(i, childRect);
		if (childRect.y < endY) {
		    if ((childRect.y + childRect.height) >= clip.y) {
			listPainter.paint(g, childRect.x, childRect.y,
					  childRect.width, childRect.height,
					  this, i);
		    }
		}
		else {
		    break;
		}
	    }
	}
    }

    /**
     * Paints one of the children; called by paint().  By default
     * that is all it does, but a subclass can use this to paint
     * things relative to the child.
     *
     * @param g the graphics context
     * @param alloc the allocated region to render the child into
     * @param index the index of the child
     */
    protected void paintChild(Graphics g, Rectangle alloc, int index) {
	listPainter.paint(g, alloc.x, alloc.y, alloc.width, alloc.height, this, index);
	super.paintChild(g, alloc, index);
    }

    protected void setPropertiesFromAttributes() {
	super.setPropertiesFromAttributes();
	listPainter = getStyleSheet().getListPainter(getAttributes());
    }

    private StyleSheet.ListPainter listPainter;
}