/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.phontalk.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.CommonTree;


/**
 * Utility methods for working with antlr3 {@link CommonTree}s.
 *
 */
public class AntlrUtils {
	
	/**
	 * Create a new common tree node
	 * with the given token type.
	 */
	public static CommonTree createToken(AntlrTokens tokens, String tokenName) {
		return createToken(tokens.getTokenType(tokenName));
	}
	
	public static CommonTree createToken(int tokenType) {
		return new CommonTree(new CommonToken(tokenType));
	}
	
	public static void printTree(CommonTree tree) {
		final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
		printTree(chatTokens, tree, 0);
	}
	
	public static void printTree(AntlrTokens chatTokens, CommonTree tree, int indent) {
		if(tree != null) {
			for(int i = 0; i < indent; i++) System.out.print("    ");
			System.out.print(chatTokens.getTokenName(tree.getToken().getType()));
			if(tree.getToken().getText() != null) {
				System.out.print(":" + tree.getToken().getText());
			}
			System.out.println();
		}
		indent++;
		for(int i = 0; i < tree.getChildCount(); i++) {
			CommonTree child = (CommonTree)tree.getChild(i);
			printTree(chatTokens, child, indent);
		}
	}
	
	/**
	 * add a text node
	 */
	public static void addTextNode(CommonTree parent, AntlrTokens tokens, String data) {
		CommonTree txtNode = 
			AntlrUtils.createToken(tokens, "TEXT");
		txtNode.getToken().setText(data);
		txtNode.setParent(parent);
		parent.addChild(txtNode);
	}
	
	/**
	 * Find all children with the given type in the provided
	 * tree.  Will only check 1st level children (i.e., this
	 * method is not recursive.)
	 * 
	 * @param tree
	 * @param type
	 * @return all children found with the given token type
	 */
	public static List<CommonTree> findChildrenWithType(CommonTree tree, int type) {
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		
		for(int i = 0; i < tree.getChildCount(); i++) {
			final CommonTree child = (CommonTree)tree.getChild(i);
			if(child.getToken().getType() == type) {
				retVal.add(child);
			}
		}
		
		return retVal;
	}

	/**
	 * Find all children with the given type in the provided
	 * tree.  Will only check 1st level children (i.e., this
	 * method is not recursive.)
	 * 
	 * @param tree
	 * @param tokens
	 * @param typeName
	 * @return all children found with the given token type
	 */
	public static List<CommonTree> findChildrenWithType(CommonTree tree, AntlrTokens tokens, String typeName) {
		return findChildrenWithType(tree, tokens.getTokenType(typeName));
	}
	
	/**
	 * Find all children and sub-children with the given type.
	 * This method will recursivly search the children of the
	 * given tree.
	 * 
	 * @param tree
	 * @param type
	 * @return the list of children found with the given type
	 */
	public static List<CommonTree> findAllChildrenWithType(CommonTree tree, int type) {
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		findAllChildrenRecursive(retVal, tree, Collections.singletonList(type));
		return retVal;
	}
	
	/**
	 * Find all children and sub-children with the given type.
	 * This method will recursively search the children of the
	 * given tree.
	 * 
	 * @param tree
	 * @param tokens
	 * @param typeName
	 * @return the list of children found with the given type
	 */
	public static List<CommonTree> findAllChildrenWithType(CommonTree tree, AntlrTokens tokens, String typeName) {
		return findAllChildrenWithType(tree, tokens.getTokenType(typeName));
	}
	
	/**
	 * Find all children and sub-children with the given type.
	 * This method will recursively search the children of the
	 * given tree.
	 * 
	 * @param tree
	 * @param tokens
	 * @param typeName
	 * @return the list of children found with the given type
	 */
	public static List<CommonTree> findAllChildrenWithType(CommonTree tree, AntlrTokens tokens, String... typeNames) {
		final List<Integer> allowedVals = new ArrayList<Integer>();
		for(String typeName:typeNames) {
			allowedVals.add(tokens.getTokenType(typeName));
		}
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		findAllChildrenRecursive(retVal, tree, allowedVals);
		return retVal;
	}
	
	private static void findAllChildrenRecursive(List<CommonTree> list, CommonTree tree, List<Integer> types) {
		// add current tree if it has the propert type
		if(types.contains(tree.getToken().getType())) {
			list.add(tree);
		}
		
		for(int i = 0; i < tree.getChildCount(); i++) {
			final CommonTree child = (CommonTree)tree.getChild(i);
			findAllChildrenRecursive(list, child, types);
		}
	}
	
	
}
